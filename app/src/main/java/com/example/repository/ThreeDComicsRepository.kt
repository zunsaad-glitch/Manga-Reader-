package com.example.repository

import com.example.api.ChapterAttributes
import com.example.api.ChapterData
import com.example.api.MangaAttributes
import com.example.api.MangaData
import com.example.api.Relationship
import com.example.api.RelationshipAttributes

data class ThreeDChapter(
    val chapterId: String,
    val mangaId: String,
    val chapterNumber: String,
    val title: String,
    val mediaId: String,
    val pageCount: Int,
    val publishAt: String
)

data class ThreeDManga(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val contentRating: String,
    val chapters: List<ThreeDChapter>
) {
    val coverUrl: String
        get() {
            val firstMediaId = chapters.firstOrNull()?.mediaId ?: "2279150"
            return "https://t.nhentai.net/galleries/$firstMediaId/thumb.jpg"
        }
}

object ThreeDComicsRepository {

    private val mangas: List<ThreeDManga> = listOf(
        ThreeDManga(
            id = "3d_kunoichi",
            title = "[Studio FOW] Kunoichi 3DCG Chronicles [Full Color 3D]",
            author = "Studio FOW",
            description = "High-end 3D rendered ninja espionage action comic in cinematic full color. Follow the shadow operative through stealth missions and clan trials.",
            contentRating = "pornographic",
            chapters = listOf(
                ThreeDChapter(
                    chapterId = "3d_kunoichi_ch1",
                    mangaId = "3d_kunoichi",
                    chapterNumber = "1",
                    title = "Episode 1: Infiltration & The Shadow Clan [3DCG]",
                    mediaId = "2279150",
                    pageCount = 32,
                    publishAt = "2024-01-10T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_kunoichi_ch2",
                    mangaId = "3d_kunoichi",
                    chapterNumber = "2",
                    title = "Episode 2: Crimson Scroll & Deep Recon [3DCG]",
                    mediaId = "2348120",
                    pageCount = 34,
                    publishAt = "2024-01-24T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_kunoichi_ch3",
                    mangaId = "3d_kunoichi",
                    chapterNumber = "3",
                    title = "Episode 3: Moonlight Ambush & Forbidden Jutsu [3DCG]",
                    mediaId = "2419850",
                    pageCount = 36,
                    publishAt = "2024-02-07T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_kunoichi_ch4",
                    mangaId = "3d_kunoichi",
                    chapterNumber = "4",
                    title = "Episode 4: Fortress Dungeon & Master's Trial [3DCG]",
                    mediaId = "2485120",
                    pageCount = 38,
                    publishAt = "2024-02-21T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_kunoichi_ch5",
                    mangaId = "3d_kunoichi",
                    chapterNumber = "5",
                    title = "Episode 5: Final Dawn & Total Mastery [Full Color 3D]",
                    mediaId = "2541290",
                    pageCount = 44,
                    publishAt = "2024-03-06T12:00:00+00:00"
                )
            )
        ),
        ThreeDManga(
            id = "3d_re_claire",
            title = "[3D Fantasy] Resident Evil - Claire & Jill 3DCG Special [English]",
            author = "3D Fantasy Renders",
            description = "Survival horror 3D graphic novel featuring Claire Redfield and Jill Valentine escaping Raccoon City's underground outbreak.",
            contentRating = "pornographic",
            chapters = listOf(
                ThreeDChapter(
                    chapterId = "3d_re_claire_ch1",
                    mangaId = "3d_re_claire",
                    chapterNumber = "1",
                    title = "Act 1: R.P.D. Basement Infiltration - Claire [3DCG]",
                    mediaId = "2498210",
                    pageCount = 34,
                    publishAt = "2024-01-12T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_re_claire_ch2",
                    mangaId = "3d_re_claire",
                    chapterNumber = "2",
                    title = "Act 2: Downtown Evacuation - Jill Valentine [3DCG]",
                    mediaId = "2560120",
                    pageCount = 36,
                    publishAt = "2024-01-26T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_re_claire_ch3",
                    mangaId = "3d_re_claire",
                    chapterNumber = "3",
                    title = "Act 3: Underground Laboratory Secrets [3DCG]",
                    mediaId = "2610450",
                    pageCount = 38,
                    publishAt = "2024-02-09T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_re_claire_ch4",
                    mangaId = "3d_re_claire",
                    chapterNumber = "4",
                    title = "Act 4: Nemesis Encounter & Sewer Escape [3DCG]",
                    mediaId = "2665120",
                    pageCount = 32,
                    publishAt = "2024-02-23T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_re_claire_ch5",
                    mangaId = "3d_re_claire",
                    chapterNumber = "5",
                    title = "Act 5: Final Extraction at Sunrise [3DCG]",
                    mediaId = "2719120",
                    pageCount = 40,
                    publishAt = "2024-03-08T12:00:00+00:00"
                )
            )
        ),
        ThreeDManga(
            id = "3d_cyberpunk",
            title = "[Project 3D] Cyberpunk 2077 - Night City Stories 3DCG",
            author = "Project 3D",
            description = "Photorealistic 3D graphic comic set in neon-drenched Night City. High-stakes cyberware contracts and netrunner intrigue.",
            contentRating = "erotica",
            chapters = listOf(
                ThreeDChapter(
                    chapterId = "3d_cyberpunk_ch1",
                    mangaId = "3d_cyberpunk",
                    chapterNumber = "1",
                    title = "Gig 1: Neon Afterlife - Watson Briefing [3DCG]",
                    mediaId = "1748231",
                    pageCount = 34,
                    publishAt = "2024-01-14T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_cyberpunk_ch2",
                    mangaId = "3d_cyberpunk",
                    chapterNumber = "2",
                    title = "Gig 2: Braindance Sessions at Lizzie's Bar [3DCG]",
                    mediaId = "1149201",
                    pageCount = 32,
                    publishAt = "2024-01-28T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_cyberpunk_ch3",
                    mangaId = "3d_cyberpunk",
                    chapterNumber = "3",
                    title = "Gig 3: Pacifica Night Run & Netrunner Intrigue [3DCG]",
                    mediaId = "1287450",
                    pageCount = 36,
                    publishAt = "2024-02-11T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_cyberpunk_ch4",
                    mangaId = "3d_cyberpunk",
                    chapterNumber = "4",
                    title = "Gig 4: Corpo Plaza Penthouse Infiltration [3DCG]",
                    mediaId = "1450912",
                    pageCount = 34,
                    publishAt = "2024-02-25T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_cyberpunk_ch5",
                    mangaId = "3d_cyberpunk",
                    chapterNumber = "5",
                    title = "Gig 5: Night City Climax & Sunset Drive [3DCG]",
                    mediaId = "1553421",
                    pageCount = 38,
                    publishAt = "2024-03-10T12:00:00+00:00"
                )
            )
        ),
        ThreeDManga(
            id = "3d_tifa_ff7",
            title = "[Cherry Bomb] Final Fantasy VII - Tifa & Aerith 3D Rendered Comic",
            author = "Cherry Bomb 3D",
            description = "Stunning 3D CG comic capturing Sector 7 Seventh Heaven nightlife and Cloud's companion adventures.",
            contentRating = "pornographic",
            chapters = listOf(
                ThreeDChapter(
                    chapterId = "3d_tifa_ff7_ch1",
                    mangaId = "3d_tifa_ff7",
                    chapterNumber = "1",
                    title = "Chapter 1: Seventh Heaven Night Shift - Tifa Lockhart [3DCG]",
                    mediaId = "1614210",
                    pageCount = 38,
                    publishAt = "2024-01-15T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_tifa_ff7_ch2",
                    mangaId = "3d_tifa_ff7",
                    chapterNumber = "2",
                    title = "Chapter 2: Sector 7 Church Exploration - Aerith [3DCG]",
                    mediaId = "1884210",
                    pageCount = 30,
                    publishAt = "2024-01-29T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_tifa_ff7_ch3",
                    mangaId = "3d_tifa_ff7",
                    chapterNumber = "3",
                    title = "Chapter 3: Wall Market Rendezvous & Honey Bee Special [3DCG]",
                    mediaId = "1965890",
                    pageCount = 28,
                    publishAt = "2024-02-12T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_tifa_ff7_ch4",
                    mangaId = "3d_tifa_ff7",
                    chapterNumber = "4",
                    title = "Chapter 4: Shinra Tower Rooftop Infiltration [3DCG]",
                    mediaId = "2054120",
                    pageCount = 34,
                    publishAt = "2024-02-26T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_tifa_ff7_ch5",
                    mangaId = "3d_tifa_ff7",
                    chapterNumber = "5",
                    title = "Chapter 5: Costa del Sol Vacation Special [Full 3D]",
                    mediaId = "2065123",
                    pageCount = 42,
                    publishAt = "2024-03-11T12:00:00+00:00"
                )
            )
        ),
        ThreeDManga(
            id = "3d_milftoon_suburban",
            title = "[MilfToon] Suburban Secrets - 3DCG Graphic Novel Series",
            author = "MilfToon Renders",
            description = "Classic Western 3D graphic novel series exploring neighborhood intrigue, poolside parties, and private drama.",
            contentRating = "pornographic",
            chapters = listOf(
                ThreeDChapter(
                    chapterId = "3d_milftoon_suburban_ch1",
                    mangaId = "3d_milftoon_suburban",
                    chapterNumber = "1",
                    title = "Vol. 1: Suburban Secrets - Moving In [3DCG]",
                    mediaId = "2019715",
                    pageCount = 36,
                    publishAt = "2024-01-16T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_milftoon_suburban_ch2",
                    mangaId = "3d_milftoon_suburban",
                    chapterNumber = "2",
                    title = "Vol. 2: Neighborhood Welcome Party & Late Cocktails [3DCG]",
                    mediaId = "1996892",
                    pageCount = 32,
                    publishAt = "2024-01-30T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_milftoon_suburban_ch3",
                    mangaId = "3d_milftoon_suburban",
                    chapterNumber = "3",
                    title = "Vol. 3: Summer Pool Days & Backyard Confessions [3DCG]",
                    mediaId = "1276626",
                    pageCount = 34,
                    publishAt = "2024-02-13T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_milftoon_suburban_ch4",
                    mangaId = "3d_milftoon_suburban",
                    chapterNumber = "4",
                    title = "Vol. 4: Late Night Gossip & The Garage Workshop [3DCG]",
                    mediaId = "1870727",
                    pageCount = 30,
                    publishAt = "2024-02-27T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_milftoon_suburban_ch5",
                    mangaId = "3d_milftoon_suburban",
                    chapterNumber = "5",
                    title = "Vol. 5: Block Party Finale & Sunset Celebration [3DCG]",
                    mediaId = "2149589",
                    pageCount = 38,
                    publishAt = "2024-03-12T12:00:00+00:00"
                )
            )
        ),
        ThreeDManga(
            id = "3d_sinful_dreams",
            title = "[Studio Lust] 3D Sinful Dreams - Luxury Graphic Novel",
            author = "Studio Lust",
            description = "Full-color 3D graphic webtoon following luxury penthouse romance, high society parties, and midnight secrets.",
            contentRating = "pornographic",
            chapters = listOf(
                ThreeDChapter(
                    chapterId = "3d_sinful_dreams_ch1",
                    mangaId = "3d_sinful_dreams",
                    chapterNumber = "1",
                    title = "Episode 1: Luxury Penthouse Invitation & VIP Suite [3DCG]",
                    mediaId = "1912020",
                    pageCount = 34,
                    publishAt = "2024-01-18T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_sinful_dreams_ch2",
                    mangaId = "3d_sinful_dreams",
                    chapterNumber = "2",
                    title = "Episode 2: Private Yacht Weekend & Sunset Gala [3DCG]",
                    mediaId = "1862462",
                    pageCount = 32,
                    publishAt = "2024-02-01T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_sinful_dreams_ch3",
                    mangaId = "3d_sinful_dreams",
                    chapterNumber = "3",
                    title = "Episode 3: Midnight Whispers & Secret Desires [3DCG]",
                    mediaId = "1428040",
                    pageCount = 30,
                    publishAt = "2024-02-15T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_sinful_dreams_ch4",
                    mangaId = "3d_sinful_dreams",
                    chapterNumber = "4",
                    title = "Episode 4: The Masquerade Ball & Champagne Toast [3DCG]",
                    mediaId = "2000543",
                    pageCount = 36,
                    publishAt = "2024-03-01T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_sinful_dreams_ch5",
                    mangaId = "3d_sinful_dreams",
                    chapterNumber = "5",
                    title = "Episode 5: Morning After & New Beginnings [3DCG]",
                    mediaId = "1199832",
                    pageCount = 38,
                    publishAt = "2024-03-15T12:00:00+00:00"
                )
            )
        ),
        ThreeDManga(
            id = "3d_witch_hunter",
            title = "[RedGrave] Witch Hunter - 3D Western Graphic Novel [English]",
            author = "RedGrave 3D",
            description = "Dark fantasy 3D graphic novel featuring monster hunters, enchanted castles, and ancient covenants.",
            contentRating = "erotica",
            chapters = listOf(
                ThreeDChapter(
                    chapterId = "3d_witch_hunter_ch1",
                    mangaId = "3d_witch_hunter",
                    chapterNumber = "1",
                    title = "Chapter 1: The Dark Forest & The Silver Cross [3DCG]",
                    mediaId = "1754820",
                    pageCount = 36,
                    publishAt = "2024-01-20T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_witch_hunter_ch2",
                    mangaId = "3d_witch_hunter",
                    chapterNumber = "2",
                    title = "Chapter 2: The Alchemist's Tower & Enchantress Pact [3DCG]",
                    mediaId = "1478120",
                    pageCount = 34,
                    publishAt = "2024-02-03T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_witch_hunter_ch3",
                    mangaId = "3d_witch_hunter",
                    chapterNumber = "3",
                    title = "Chapter 3: Blood Moon Ritual in Ancient Crypts [3DCG]",
                    mediaId = "1568210",
                    pageCount = 32,
                    publishAt = "2024-02-17T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_witch_hunter_ch4",
                    mangaId = "3d_witch_hunter",
                    chapterNumber = "4",
                    title = "Chapter 4: The Citadel Siege & Queen's Chamber [3DCG]",
                    mediaId = "1661200",
                    pageCount = 36,
                    publishAt = "2024-03-02T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_witch_hunter_ch5",
                    mangaId = "3d_witch_hunter",
                    chapterNumber = "5",
                    title = "Chapter 5: Huntress Dawn & The New Order [3DCG]",
                    mediaId = "1870120",
                    pageCount = 40,
                    publishAt = "2024-03-16T12:00:00+00:00"
                )
            )
        ),
        ThreeDManga(
            id = "3d_supergirl",
            title = "[3D Hentai] Supergirl & Power Girl - 3DCG Comics Anthology",
            author = "CG Heroes",
            description = "Superheroine comic rendered in high-definition 3D CG modeling. Watch the cousins patrol Metropolis and team up against villains.",
            contentRating = "pornographic",
            chapters = listOf(
                ThreeDChapter(
                    chapterId = "3d_supergirl_ch1",
                    mangaId = "3d_supergirl",
                    chapterNumber = "1",
                    title = "Issue 1: Fortress of Solitude Healing Chambers [3DCG]",
                    mediaId = "1325789",
                    pageCount = 32,
                    publishAt = "2024-01-22T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_supergirl_ch2",
                    mangaId = "3d_supergirl",
                    chapterNumber = "2",
                    title = "Issue 2: Metropolis Skyline Night Patrol [3DCG]",
                    mediaId = "1414320",
                    pageCount = 34,
                    publishAt = "2024-02-05T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_supergirl_ch3",
                    mangaId = "3d_supergirl",
                    chapterNumber = "3",
                    title = "Issue 3: Sun-Dipping on Earth's Orbit [3DCG]",
                    mediaId = "2100340",
                    pageCount = 30,
                    publishAt = "2024-02-19T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_supergirl_ch4",
                    mangaId = "3d_supergirl",
                    chapterNumber = "4",
                    title = "Issue 4: LexCorp Tower Secret Laboratory [3DCG]",
                    mediaId = "1812340",
                    pageCount = 36,
                    publishAt = "2024-03-04T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_supergirl_ch5",
                    mangaId = "3d_supergirl",
                    chapterNumber = "5",
                    title = "Issue 5: Penthouse R&R After The Crisis [3DCG]",
                    mediaId = "1834590",
                    pageCount = 38,
                    publishAt = "2024-03-18T12:00:00+00:00"
                )
            )
        ),
        ThreeDManga(
            id = "3d_overwatch",
            title = "[RenderVerse] Overwatch - Mercy & D.Va 3D Chronicles [Full Color]",
            author = "RenderVerse",
            description = "Sci-fi 3D rendered graphic story of heroines taking downtime between peacekeeping missions.",
            contentRating = "pornographic",
            chapters = listOf(
                ThreeDChapter(
                    chapterId = "3d_overwatch_ch1",
                    mangaId = "3d_overwatch",
                    chapterNumber = "1",
                    title = "Mission 1: Watchpoint Gibraltar Downtime - Mercy & D.Va [3DCG]",
                    mediaId = "1852100",
                    pageCount = 32,
                    publishAt = "2024-01-25T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_overwatch_ch2",
                    mangaId = "3d_overwatch",
                    chapterNumber = "2",
                    title = "Mission 2: Hanamura Cherry Blossom Festival [3DCG]",
                    mediaId = "1924100",
                    pageCount = 30,
                    publishAt = "2024-02-08T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_overwatch_ch3",
                    mangaId = "3d_overwatch",
                    chapterNumber = "3",
                    title = "Mission 3: King's Row Night Patrol & MEKA Tune-up [3DCG]",
                    mediaId = "1935120",
                    pageCount = 34,
                    publishAt = "2024-02-22T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_overwatch_ch4",
                    mangaId = "3d_overwatch",
                    chapterNumber = "4",
                    title = "Mission 4: Oasis Research Labs Retreat [3DCG]",
                    mediaId = "1945110",
                    pageCount = 32,
                    publishAt = "2024-03-07T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_overwatch_ch5",
                    mangaId = "3d_overwatch",
                    chapterNumber = "5",
                    title = "Mission 5: Victory Celebration at Headquarters [3DCG]",
                    mediaId = "1984210",
                    pageCount = 36,
                    publishAt = "2024-03-21T12:00:00+00:00"
                )
            )
        ),
        ThreeDManga(
            id = "3d_raiden_genshin",
            title = "[CG Comic] Genshin Impact - Raiden Shogun & Yae Miko 3DCG Story",
            author = "Genshin 3D Studio",
            description = "Inazuma realm 3D graphic comic in vivid high resolution. The Electro Archon and the Guuji of Grand Narukami Shrine.",
            contentRating = "pornographic",
            chapters = listOf(
                ThreeDChapter(
                    chapterId = "3d_raiden_genshin_ch1",
                    mangaId = "3d_raiden_genshin",
                    chapterNumber = "1",
                    title = "Part 1: Tenshukaku Palace - The Shogun's Reflection [3DCG]",
                    mediaId = "2024560",
                    pageCount = 32,
                    publishAt = "2024-01-27T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_raiden_genshin_ch2",
                    mangaId = "3d_raiden_genshin",
                    chapterNumber = "2",
                    title = "Part 2: Grand Narukami Shrine - Yae Miko's Teasing [3DCG]",
                    mediaId = "2118910",
                    pageCount = 30,
                    publishAt = "2024-02-10T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_raiden_genshin_ch3",
                    mangaId = "3d_raiden_genshin",
                    chapterNumber = "3",
                    title = "Part 3: Chinju Forest Midnight Walk & Danuki Magic [3DCG]",
                    mediaId = "2135110",
                    pageCount = 34,
                    publishAt = "2024-02-24T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_raiden_genshin_ch4",
                    mangaId = "3d_raiden_genshin",
                    chapterNumber = "4",
                    title = "Part 4: Plane of Euthymia - Inner Harmony [3DCG]",
                    mediaId = "2199450",
                    pageCount = 32,
                    publishAt = "2024-03-09T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_raiden_genshin_ch5",
                    mangaId = "3d_raiden_genshin",
                    chapterNumber = "5",
                    title = "Part 5: Inazuma Festival Fireworks Celebration [3DCG]",
                    mediaId = "2215890",
                    pageCount = 36,
                    publishAt = "2024-03-23T12:00:00+00:00"
                )
            )
        ),
        ThreeDManga(
            id = "3d_samus_metroid",
            title = "[Studio Ethereal] Metroid - Samus Aran 3DCG Mission [Full 3D]",
            author = "Studio Ethereal",
            description = "Galactic bounty hunter Samus Aran in a cinematic 3D graphic novel adventure deep beneath alien planetary installations.",
            contentRating = "erotica",
            chapters = listOf(
                ThreeDChapter(
                    chapterId = "3d_samus_metroid_ch1",
                    mangaId = "3d_samus_metroid",
                    chapterNumber = "1",
                    title = "Log 1: Zero Suit Calibration in the Gunship [3DCG]",
                    mediaId = "2258120",
                    pageCount = 30,
                    publishAt = "2024-01-28T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_samus_metroid_ch2",
                    mangaId = "3d_samus_metroid",
                    chapterNumber = "2",
                    title = "Log 2: Sector Zero Infiltration & Chozo Ruins [3DCG]",
                    mediaId = "2319830",
                    pageCount = 32,
                    publishAt = "2024-02-11T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_samus_metroid_ch3",
                    mangaId = "3d_samus_metroid",
                    chapterNumber = "3",
                    title = "Log 3: Magmoor Caverns Thermal Spring [3DCG]",
                    mediaId = "2368940",
                    pageCount = 34,
                    publishAt = "2024-02-25T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_samus_metroid_ch4",
                    mangaId = "3d_samus_metroid",
                    chapterNumber = "4",
                    title = "Log 4: Mother Brain Core Decontamination [3DCG]",
                    mediaId = "2381290",
                    pageCount = 32,
                    publishAt = "2024-03-10T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_samus_metroid_ch5",
                    mangaId = "3d_samus_metroid",
                    chapterNumber = "5",
                    title = "Log 5: Mission Debriefing at Galactic Station [3DCG]",
                    mediaId = "2429120",
                    pageCount = 36,
                    publishAt = "2024-03-24T12:00:00+00:00"
                )
            )
        ),
        ThreeDManga(
            id = "3d_2b_nier",
            title = "[DreamCatcher] Nier Automata - 2B Combat & Intimacy 3DCG [Full Color]",
            author = "DreamCatcher 3D",
            description = "Android YoRHa 2B in a deep emotional 3D graphic novel adaptation exploring human emotion, combat memories, and devotion.",
            contentRating = "pornographic",
            chapters = listOf(
                ThreeDChapter(
                    chapterId = "3d_2b_nier_ch1",
                    mangaId = "3d_2b_nier",
                    chapterNumber = "1",
                    title = "Route A: YoRHa Bunker Maintenance & Scanner Sync [3DCG]",
                    mediaId = "2458120",
                    pageCount = 32,
                    publishAt = "2024-01-30T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_2b_nier_ch2",
                    mangaId = "3d_2b_nier",
                    chapterNumber = "2",
                    title = "Route B: City Ruins Oasis & Desert Camp Recon [3DCG]",
                    mediaId = "2478910",
                    pageCount = 30,
                    publishAt = "2024-02-13T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_2b_nier_ch3",
                    mangaId = "3d_2b_nier",
                    chapterNumber = "3",
                    title = "Route C: Forest Kingdom Secrets & Amusement Park [3DCG]",
                    mediaId = "2516780",
                    pageCount = 34,
                    publishAt = "2024-02-27T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_2b_nier_ch4",
                    mangaId = "3d_2b_nier",
                    chapterNumber = "4",
                    title = "Route D: Flooded City Twilight Reconnaissance [3DCG]",
                    mediaId = "2529810",
                    pageCount = 32,
                    publishAt = "2024-03-12T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_2b_nier_ch5",
                    mangaId = "3d_2b_nier",
                    chapterNumber = "5",
                    title = "Route E: Pod Protocol & Soul Transference [3DCG]",
                    mediaId = "2587890",
                    pageCount = 36,
                    publishAt = "2024-03-26T12:00:00+00:00"
                )
            )
        ),
        ThreeDManga(
            id = "3d_ahsoka_starwars",
            title = "[Apex 3D] Star Wars - Ahsoka Tano 3DCG Graphic Series",
            author = "Apex 3D",
            description = "Sci-fi Jedi adventure in full 3D graphic rendering. Ahsoka journeys through forgotten temples and ancient Force realms.",
            contentRating = "erotica",
            chapters = listOf(
                ThreeDChapter(
                    chapterId = "3d_ahsoka_starwars_ch1",
                    mangaId = "3d_ahsoka_starwars",
                    chapterNumber = "1",
                    title = "Episode 1: The Jedi Archive Remnants & Saber Training [3DCG]",
                    mediaId = "2648920",
                    pageCount = 32,
                    publishAt = "2024-02-02T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_ahsoka_starwars_ch2",
                    mangaId = "3d_ahsoka_starwars",
                    chapterNumber = "2",
                    title = "Episode 2: Lothal Temple Secrets & White Sabers [3DCG]",
                    mediaId = "2731230",
                    pageCount = 30,
                    publishAt = "2024-02-16T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_ahsoka_starwars_ch3",
                    mangaId = "3d_ahsoka_starwars",
                    chapterNumber = "3",
                    title = "Episode 3: Coruscant Lower Levels Cantina Recon [3DCG]",
                    mediaId = "2754120",
                    pageCount = 34,
                    publishAt = "2024-03-01T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_ahsoka_starwars_ch4",
                    mangaId = "3d_ahsoka_starwars",
                    chapterNumber = "4",
                    title = "Episode 4: Mandalorian Outpost Alliance [3DCG]",
                    mediaId = "2798210",
                    pageCount = 32,
                    publishAt = "2024-03-15T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_ahsoka_starwars_ch5",
                    mangaId = "3d_ahsoka_starwars",
                    chapterNumber = "5",
                    title = "Episode 5: The World Between Worlds Journey [3DCG]",
                    mediaId = "2849062",
                    pageCount = 36,
                    publishAt = "2024-03-29T12:00:00+00:00"
                )
            )
        ),
        ThreeDManga(
            id = "3d_chunli_sf",
            title = "[CG Master] Street Fighter - Chun-Li & Cammy 3DCG Comic",
            author = "CG Master",
            description = "Martial arts heroines Chun-Li and Cammy in high-detail 3D CG art investigating Interpol cases and sparring.",
            contentRating = "pornographic",
            chapters = listOf(
                ThreeDChapter(
                    chapterId = "3d_chunli_sf_ch1",
                    mangaId = "3d_chunli_sf",
                    chapterNumber = "1",
                    title = "Round 1: Metro City Dojo - Morning Training [3DCG]",
                    mediaId = "2849381",
                    pageCount = 32,
                    publishAt = "2024-02-04T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_chunli_sf_ch2",
                    mangaId = "3d_chunli_sf",
                    chapterNumber = "2",
                    title = "Round 2: Cammy's British Intelligence Joint Mission [3DCG]",
                    mediaId = "2849391",
                    pageCount = 30,
                    publishAt = "2024-02-18T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_chunli_sf_ch3",
                    mangaId = "3d_chunli_sf",
                    chapterNumber = "3",
                    title = "Round 3: Shadaloo Underground Arena Fight [3DCG]",
                    mediaId = "2949057",
                    pageCount = 34,
                    publishAt = "2024-03-03T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_chunli_sf_ch4",
                    mangaId = "3d_chunli_sf",
                    chapterNumber = "4",
                    title = "Round 4: Hot Spring Recovery & Post-Sparring Talk [3DCG]",
                    mediaId = "2999240",
                    pageCount = 32,
                    publishAt = "2024-03-17T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_chunli_sf_ch5",
                    mangaId = "3d_chunli_sf",
                    chapterNumber = "5",
                    title = "Round 5: World Warrior Tournament Celebration [3DCG]",
                    mediaId = "3031776",
                    pageCount = 36,
                    publishAt = "2024-03-31T12:00:00+00:00"
                )
            )
        ),
        ThreeDManga(
            id = "3d_lara_tombraider",
            title = "[3D Realm] Tomb Raider - Lara Croft 3D Adventures [English]",
            author = "3D Realm",
            description = "Lara Croft exploring ancient Mesoamerican sunken ruins, deciphering golden glyphs in 3D graphic novel form.",
            contentRating = "erotica",
            chapters = listOf(
                ThreeDChapter(
                    chapterId = "3d_lara_tombraider_ch1",
                    mangaId = "3d_lara_tombraider",
                    chapterNumber = "1",
                    title = "Tomb 1: Croft Manor Secret Library Vault [3DCG]",
                    mediaId = "3412039",
                    pageCount = 30,
                    publishAt = "2024-02-06T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_lara_tombraider_ch2",
                    mangaId = "3d_lara_tombraider",
                    chapterNumber = "2",
                    title = "Tomb 2: Mesoamerican Sun Temple Exploration [3DCG]",
                    mediaId = "3524108",
                    pageCount = 32,
                    publishAt = "2024-02-20T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_lara_tombraider_ch3",
                    mangaId = "3d_lara_tombraider",
                    chapterNumber = "3",
                    title = "Tomb 3: Cenote Waterfall & Underground Oasis [3DCG]",
                    mediaId = "3649585",
                    pageCount = 34,
                    publishAt = "2024-03-05T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_lara_tombraider_ch4",
                    mangaId = "3d_lara_tombraider",
                    chapterNumber = "4",
                    title = "Tomb 4: The Golden Idol Chamber & Trap Escape [3DCG]",
                    mediaId = "3751651",
                    pageCount = 32,
                    publishAt = "2024-03-19T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_lara_tombraider_ch5",
                    mangaId = "3d_lara_tombraider",
                    chapterNumber = "5",
                    title = "Tomb 5: Expedition Complete & Campfire Rest [3DCG]",
                    mediaId = "3776908",
                    pageCount = 36,
                    publishAt = "2024-04-02T12:00:00+00:00"
                )
            )
        ),
        ThreeDManga(
            id = "3d_kasumi_doa",
            title = "[Digital CG] Dead or Alive - Kasumi & Honoka 3DCG Special",
            author = "Digital CG",
            description = "Island resort vacation, beach volleyball, and ninja techniques rendered in high-end 3D perfection.",
            contentRating = "pornographic",
            chapters = listOf(
                ThreeDChapter(
                    chapterId = "3d_kasumi_doa_ch1",
                    mangaId = "3d_kasumi_doa",
                    chapterNumber = "1",
                    title = "Stage 1: Zack Island Beach Resort Welcome [3DCG]",
                    mediaId = "4160998",
                    pageCount = 32,
                    publishAt = "2024-02-08T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_kasumi_doa_ch2",
                    mangaId = "3d_kasumi_doa",
                    chapterNumber = "2",
                    title = "Stage 2: Honoka & Kasumi Vacation Sparring [3DCG]",
                    mediaId = "1700474",
                    pageCount = 30,
                    publishAt = "2024-02-22T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_kasumi_doa_ch3",
                    mangaId = "3d_kasumi_doa",
                    chapterNumber = "3",
                    title = "Stage 3: Sunset Pool Party & Ninja Secrets [3DCG]",
                    mediaId = "1783739",
                    pageCount = 34,
                    publishAt = "2024-03-07T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_kasumi_doa_ch4",
                    mangaId = "3d_kasumi_doa",
                    chapterNumber = "4",
                    title = "Stage 4: Tropical Rainforest Waterfall Retreat [3DCG]",
                    mediaId = "1300710",
                    pageCount = 32,
                    publishAt = "2024-03-21T12:00:00+00:00"
                ),
                ThreeDChapter(
                    chapterId = "3d_kasumi_doa_ch5",
                    mangaId = "3d_kasumi_doa",
                    chapterNumber = "5",
                    title = "Stage 5: Island Fireworks & Festival Finale [3DCG]",
                    mediaId = "1111486",
                    pageCount = 36,
                    publishAt = "2024-04-04T12:00:00+00:00"
                )
            )
        )
    )

    fun getAll3DComics(): List<MangaData> {
        return mangas.map { m ->
            MangaData(
                id = m.id,
                attributes = MangaAttributes(
                    title = mapOf("en" to m.title),
                    description = mapOf("en" to m.description),
                    originalLanguage = "en",
                    contentRating = m.contentRating,
                    status = "completed"
                ),
                relationships = listOf(
                    Relationship(
                        id = m.id,
                        type = "cover_art",
                        attributes = RelationshipAttributes(fileName = m.coverUrl)
                    ),
                    Relationship(
                        id = m.id.removePrefix("3d_"),
                        type = "author",
                        attributes = RelationshipAttributes(name = m.author)
                    )
                )
            )
        }
    }

    fun is3DManga(mangaId: String): Boolean {
        return mangas.any { it.id == mangaId }
    }

    fun getChaptersForManga(mangaId: String): List<ChapterData>? {
        val targetManga = mangas.find { it.id == mangaId } ?: return null
        return targetManga.chapters.map { ch ->
            ChapterData(
                id = ch.chapterId,
                type = "chapter",
                attributes = ChapterAttributes(
                    volume = "1",
                    chapter = ch.chapterNumber,
                    title = ch.title,
                    translatedLanguage = "en",
                    pages = ch.pageCount,
                    publishAt = ch.publishAt
                )
            )
        }
    }

    fun is3DChapter(chapterId: String): Boolean {
        return mangas.any { m -> m.chapters.any { it.chapterId == chapterId } }
    }

    fun getPageUrlsForChapter(chapterId: String): List<String>? {
        for (m in mangas) {
            val ch = m.chapters.find { it.chapterId == chapterId }
            if (ch != null) {
                return (1..ch.pageCount).map { page ->
                    "https://i.nhentai.net/galleries/${ch.mediaId}/$page.jpg"
                }
            }
        }
        return null
    }
}
