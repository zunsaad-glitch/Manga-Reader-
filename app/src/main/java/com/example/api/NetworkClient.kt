package com.example.api

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import okhttp3.Cache
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.io.File
import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

object NetworkClient {
    @Volatile
    private var instance: OkHttpClient? = null

    // Track hosts where direct connection is blocked by ISP/firewall/DPI on this device
    private val blockedDirectHosts = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    private val PHOTON_SHARDS = listOf("i0.wp.com", "i1.wp.com", "i2.wp.com", "i3.wp.com")

    fun getPhotonUrl(url: okhttp3.HttpUrl): String {
        val shard = PHOTON_SHARDS[kotlin.math.abs(url.hashCode()) % PHOTON_SHARDS.size]
        val cleanHost = url.host
        val path = url.encodedPath
        val query = if (url.query != null) "?${url.query}" else ""
        return "https://$shard/$cleanHost$path$query"
    }

    /**
     * Bypasses ISP DNS blocking / SNI filtering (e.g. in Pakistan) by:
     * 1. Direct hardcoded bootstrap DNS mapping for CDN image servers to eliminate DNS poisoning.
     * 2. Cloudflare & Google DNS-over-HTTPS (DoH) with bootstrap IPs for all dynamic endpoints.
     * 3. Automatic browser User-Agent and Referer injection to bypass CDN hotlinking restrictions.
     */
    fun getClient(context: Context? = null): OkHttpClient {
        return instance ?: synchronized(this) {
            instance ?: buildClient(context?.applicationContext).also { instance = it }
        }
    }

    private fun buildClient(appContext: Context?): OkHttpClient {
        // Bootstrap client for DNS-over-HTTPS queries with fallback bootstrap IPs
        val bootstrapClient = OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        // Cloudflare DNS-over-HTTPS
        val cloudflareDoh = try {
            val builder = DnsOverHttps.Builder()
                .client(bootstrapClient)
                .url("https://1.1.1.1/dns-query".toHttpUrl())
                .bootstrapDnsHosts(
                    InetAddress.getByName("1.1.1.1"),
                    InetAddress.getByName("1.0.0.1"),
                    InetAddress.getByName("8.8.8.8"),
                    InetAddress.getByName("8.8.4.4"),
                    InetAddress.getByName("9.9.9.9")
                )
                .includeIPv6(false)
            builder.build()
        } catch (e: Exception) {
            Log.w("NetworkClient", "Failed to init Cloudflare DoH: ${e.message}")
            null
        }

        // Google DNS-over-HTTPS fallback
        val googleDoh = try {
            val builder = DnsOverHttps.Builder()
                .client(bootstrapClient)
                .url("https://dns.google/dns-query".toHttpUrl())
                .bootstrapDnsHosts(
                    InetAddress.getByName("8.8.8.8"),
                    InetAddress.getByName("8.8.4.4"),
                    InetAddress.getByName("1.1.1.1"),
                    InetAddress.getByName("1.0.0.1")
                )
                .includeIPv6(false)
            builder.build()
        } catch (e: Exception) {
            Log.w("NetworkClient", "Failed to init Google DoH: ${e.message}")
            null
        }

        // Hardcoded fast-path IP mapping for critical manga CDNs to completely bypass DNS tampering
        val staticIpOverrides = try {
            val nhIps = listOf(
                InetAddress.getByName("109.202.100.218"),
                InetAddress.getByName("77.247.178.1"),
                InetAddress.getByName("213.152.165.54"),
                InetAddress.getByName("213.152.165.53")
            )
            mapOf(
                "i.nhentai.net" to nhIps,
                "i1.nhentai.net" to nhIps,
                "i2.nhentai.net" to nhIps,
                "i3.nhentai.net" to nhIps,
                "i4.nhentai.net" to nhIps,
                "t.nhentai.net" to nhIps,
                "t1.nhentai.net" to nhIps,
                "t2.nhentai.net" to nhIps,
                "t3.nhentai.net" to nhIps,
                "t4.nhentai.net" to nhIps
            )
        } catch (_: Exception) {
            emptyMap()
        }

        // Combined resilient DNS - prioritizing fast native Android system DNS while rejecting ISP sinkholes
        val resilientDns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                // 1. Fast Native Android OS DNS (filter out ISP sinkhole / bogon IPs)
                try {
                    val systemResult = Dns.SYSTEM.lookup(hostname)
                    val v4 = systemResult.filter {
                        it is Inet4Address &&
                                !it.isLoopbackAddress &&
                                !it.isAnyLocalAddress &&
                                !it.isSiteLocalAddress &&
                                it.hostAddress != "0.0.0.0" &&
                                it.hostAddress != "127.0.0.1"
                    }
                    if (v4.isNotEmpty()) return v4
                    if (systemResult.isNotEmpty() && systemResult.none { it.isLoopbackAddress || it.isAnyLocalAddress }) return systemResult
                } catch (_: Exception) {}

                // 2. Cloudflare DoH fallback
                if (cloudflareDoh != null) {
                    try {
                        val result = cloudflareDoh.lookup(hostname).filter { it is Inet4Address }
                        if (result.isNotEmpty()) return result
                    } catch (_: Exception) {}
                }

                // 3. Google DoH fallback
                if (googleDoh != null) {
                    try {
                        val result = googleDoh.lookup(hostname).filter { it is Inet4Address }
                        if (result.isNotEmpty()) return result
                    } catch (_: Exception) {}
                }

                // 4. Fallback to static IP overrides
                val lowerHost = hostname.lowercase()
                staticIpOverrides[lowerHost]?.let { return it }

                throw UnknownHostException("Unable to resolve host \"$hostname\"")
            }
        }

        val dispatcher = okhttp3.Dispatcher().apply {
            maxRequests = 128
            maxRequestsPerHost = 32
        }

        val builder = OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .dns(resilientDns)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)

        // Automatic Headers Interceptor for CDN access & anti-hotlinking with automatic fallback
        builder.addInterceptor { chain ->
            val original = chain.request()
            val originalUrl = original.url
            val host = originalUrl.host.lowercase()

            val reqBuilder = original.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                .header("Accept", "image/webp,image/png,image/jpeg,image/*,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")

            when {
                host.contains("nhentai") || host.contains("cubari") -> {
                    reqBuilder.header("Referer", "https://nhentai.net/")
                }
                host.contains("pururin") -> {
                    reqBuilder.header("Referer", "https://pururin.io/")
                }
                host.contains("hentaifox") -> {
                    reqBuilder.header("Referer", "https://hentaifox.com/")
                }
                host.contains("3hentai") -> {
                    reqBuilder.header("Referer", "https://3hentai.net/")
                }
                host.contains("simply-hentai") -> {
                    reqBuilder.header("Referer", "https://simply-hentai.com/")
                }
                host.contains("asmhentai") -> {
                    reqBuilder.header("Referer", "https://asmhentai.com/")
                }
                host.contains("mangadex.org") || host.contains("mangadex") -> {
                    reqBuilder.header("Referer", "https://mangadex.org/")
                }
                host.contains("manhwatoon") -> {
                    reqBuilder.header("Referer", "https://www.manhwatoon.me/")
                }
                host.contains("mangatoon") -> {
                    reqBuilder.header("Referer", "https://mangatoon.mobi/")
                }
                else -> {
                    reqBuilder.header("Referer", "https://$host/")
                }
            }

            val path = originalUrl.encodedPath.lowercase()
            val isCdnImage = host.contains("nhentai") || host.contains("pururin") ||
                    host.contains("hentaifox") || host.contains("3hentai") ||
                    host.contains("mangadex") || host.contains("simply-hentai") ||
                    host.contains("asmhentai") || host.contains("manhwatoon") ||
                    host.contains("mangatoon") ||
                    path.endsWith(".jpg") || path.endsWith(".jpeg") ||
                    path.endsWith(".png") || path.endsWith(".webp") ||
                    path.endsWith(".gif") || path.contains("/covers/") ||
                    path.contains("/galleries/")

            // Helper to fetch via WordPress Photon edge CDN
            fun tryPhoton(): okhttp3.Response? {
                return try {
                    val photonUrl = getPhotonUrl(originalUrl)
                    val photonReq = original.newBuilder()
                        .url(photonUrl)
                        .header("Referer", "https://$host/")
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                        .build()
                    val resp = chain.proceed(photonReq)
                    if (resp.isSuccessful) resp else { resp.close(); null }
                } catch (_: Exception) {
                    null
                }
            }

            // Helper to fetch via CorsProxy.io
            fun tryCorsProxy(): okhttp3.Response? {
                return try {
                    val encodedUrl = java.net.URLEncoder.encode(originalUrl.toString(), "UTF-8")
                    val corsReq = original.newBuilder()
                        .url("https://corsproxy.io/?url=$encodedUrl")
                        .header("Referer", "https://corsproxy.io/")
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36")
                        .build()
                    val resp = chain.proceed(corsReq)
                    if (resp.isSuccessful) resp else { resp.close(); null }
                } catch (_: Exception) {
                    null
                }
            }

            // Fast-path: If this CDN host was already confirmed blocked by ISP/firewall on this device,
            // immediately bypass to Photon edge mirror to prevent any request hanging.
            if (isCdnImage && blockedDirectHosts.contains(host)) {
                tryPhoton()?.let { return@addInterceptor it }
                tryCorsProxy()?.let { return@addInterceptor it }
            }

            var response: okhttp3.Response? = null
            var requestException: Exception? = null

            try {
                val directChain = if (isCdnImage) {
                    chain.withConnectTimeout(3500, TimeUnit.MILLISECONDS)
                        .withReadTimeout(8, TimeUnit.SECONDS)
                } else {
                    chain
                }
                response = directChain.proceed(reqBuilder.build())
                if (response.isSuccessful) {
                    return@addInterceptor response
                }
            } catch (e: Exception) {
                requestException = e
                if (isCdnImage) {
                    blockedDirectHosts.add(host)
                }
            }

            // High-reliability edge proxy fallback for CDN images (covers 403, 404, 502, ISP blocking, DNS tampering)
            if (isCdnImage) {
                blockedDirectHosts.add(host)
                tryPhoton()?.let {
                    response?.close()
                    return@addInterceptor it
                }
                tryCorsProxy()?.let {
                    response?.close()
                    return@addInterceptor it
                }
            }

            if (response != null) {
                return@addInterceptor response
            }
            throw requestException ?: java.io.IOException("Failed to connect to ${original.url}")
        }

        return builder.build()
    }

    @Volatile
    private var imageLoaderInstance: coil.ImageLoader? = null

    fun getImageLoader(context: Context): coil.ImageLoader {
        return imageLoaderInstance ?: synchronized(this) {
            imageLoaderInstance ?: coil.ImageLoader.Builder(context.applicationContext)
                .okHttpClient { getClient(context) }
                .memoryCache {
                    coil.memory.MemoryCache.Builder(context.applicationContext)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    coil.disk.DiskCache.Builder()
                        .directory(context.applicationContext.cacheDir.resolve("image_cache"))
                        .maxSizeBytes(200L * 1024L * 1024L)
                        .build()
                }
                .allowHardware(false)
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .respectCacheHeaders(false)
                .crossfade(true)
                .build().also { imageLoaderInstance = it }
        }
    }
}


