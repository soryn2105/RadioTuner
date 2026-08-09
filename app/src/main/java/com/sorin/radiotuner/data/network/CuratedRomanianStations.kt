package com.sorin.radiotuner.data.network

import com.sorin.radiotuner.data.model.Station

/**
 * (RO) O listă selectată de posturi radio din România, verificate manual ca fiind stabile și populare.
 * Folosită ca sursă primară în [RomanianSpecializedRepository].
 */
object CuratedRomanianStations {
    val STATIONS = listOf(
        Station(
            id = "curated-kissfm",
            name = "Kiss FM",
            genre = "Pop / Top 40",
            country = "Romania",
            streamUrl = "https://dc.digi.live.kissfm.ro/kissfm.aacp"
        ),
        Station(
            id = "curated-radiozu",
            name = "Radio ZU",
            genre = "Pop / Divertisment",
            country = "Romania",
            streamUrl = "http://stream.radiozu.ro:8020/;stream/1"
        ),
        Station(
            id = "curated-europafm",
            name = "Europa FM",
            genre = "Știri / Adult Contemporary",
            country = "Romania",
            streamUrl = "http://astreaming.europafm.ro:8000/europafm_mp3_64k"
        ),
        Station(
            id = "curated-digifm",
            name = "Digi FM",
            genre = "Știri / Pop",
            country = "Romania",
            streamUrl = "https://edge76.rcs-rds.ro/digifm/digifm.mp3"
        ),
        Station(
            id = "curated-magicfm",
            name = "Magic FM",
            genre = "Soft Pop / 80s & 90s",
            country = "Romania",
            streamUrl = "https://dc.digi.live.kissfm.ro/magicfm.aacp"
        ),
        Station(
            id = "curated-rockfm",
            name = "Rock FM",
            genre = "Classic Rock",
            country = "Romania",
            streamUrl = "https://dc.digi.live.kissfm.ro/rockfm.aacp"
        ),
        Station(
            id = "curated-profm",
            name = "ProFM",
            genre = "Pop / Dance",
            country = "Romania",
            streamUrl = "http://stream.profm.ro:8012/profm.mp3"
        ),
        Station(
            id = "curated-rra",
            name = "Radio România Actualități",
            genre = "Știri / Serviciu Public",
            country = "Romania",
            streamUrl = "http://89.238.227.6:8008/;"
        ),
        Station(
            id = "curated-virgin",
            name = "Virgin Radio",
            genre = "Hip-Hop / R&B / Pop",
            country = "Romania",
            streamUrl = "http://astreaming.virginradio.ro:8000/virgin_mp3_64k"
        ),
        Station(
            id = "curated-guerrilla",
            name = "Radio Guerrilla",
            genre = "Alternative / Indie",
            country = "Romania",
            streamUrl = "http://live.guerrillaradio.ro:8010/guerrilla.mp3"
        ),
        Station(
            id = "curated-dancefm",
            name = "Dance FM",
            genre = "Electronic / House",
            country = "Romania",
            streamUrl = "http://89.238.227.6:8038/;"
        ),
        Station(
            id = "curated-romanticfm",
            name = "Romantic FM",
            genre = "Easy Listening",
            country = "Romania",
            streamUrl = "http://89.238.227.6:8062/;"
        ),
        Station(
            id = "curated-capital-manele",
            name = "Capital FM Romania Manele",
            genre = "Manele / Populară",
            country = "Romania",
            streamUrl = "https://ssl.omegahost.ro:8020/stream",
            imageUrl = "https://www.capitalfm.ro/logo.png",
            homepage = "https://www.capitalfm.ro"
        ),
        Station(
            id = "curated-capital-dance",
            name = "Capital FM Romania Dance",
            genre = "Dance / Club",
            country = "Romania",
            streamUrl = "https://ssl.omegahost.ro:8020/dance",
            imageUrl = "https://www.capitalfm.ro/logo.png",
            homepage = "https://www.capitalfm.ro"
        ),
        Station(
            id = "curated-capital-etno",
            name = "Capital FM Romania Etno",
            genre = "Etno / Petrecere",
            country = "Romania",
            streamUrl = "https://ssl.omegahost.ro:8020/etno",
            imageUrl = "https://www.capitalfm.ro/logo.png",
            homepage = "https://www.capitalfm.ro"
        ),
        Station(
            id = "curated-capital-hiphop",
            name = "Capital FM Hip-Hop",
            genre = "Hip-Hop / Rap",
            country = "Romania",
            streamUrl = "http://live.capitalfm.ro:8010/;"
        ),
        Station(
            id = "curated-prahova",
            name = "Radio Prahova",
            genre = "Pop / Top 40",
            country = "Romania",
            streamUrl = "http://streamx.rph.ro:8100/relay"
        ),
        Station(
            id = "curated-prahova-live",
            name = "Radio Prahova 2 (Live)",
            genre = "Pop / Top 40",
            country = "Romania",
            streamUrl = "http://ronix-live.rph.ro:8100/;"
        ),
        Station(
            id = "curated-manele-ro",
            name = "Radio Manele România",
            genre = "Manele / Party",
            country = "Romania",
            streamUrl = "https://ssl.fmradiomanele.ro:8044/"
        ),
        Station(
            id = "curated-hitfm-manele",
            name = "Radio HiT FM Manele",
            genre = "Manele",
            country = "Romania",
            streamUrl = "http://live.radiohitfm.ro:8000/stream"
        ),
        Station(
            id = "curated-taraf",
            name = "Radio Taraf",
            genre = "Manele / Petrecere",
            country = "Romania",
            streamUrl = "http://streaming.radiotaraf.ro:8000/live"
        )
    )
}
