package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class StationRepository(private val stationDao: StationDao) {

    val allStations: Flow<List<Station>> = stationDao.getAllStations()

    suspend fun insert(station: Station) {
        stationDao.insertStation(station)
    }

    suspend fun update(station: Station) {
        stationDao.updateStation(station)
    }

    suspend fun delete(station: Station) {
        stationDao.deleteStation(station)
    }

    suspend fun getStationById(id: Int): Station? {
        return stationDao.getStationById(id)
    }

    suspend fun prepopulateIfEmpty() {
        val currentList = allStations.first()
        if (currentList.isEmpty()) {
            val defaults = getDefaultStations()
            for (station in defaults) {
                stationDao.insertStation(station)
            }
        }
    }

    suspend fun syncWithLatestStations() {
        val currentList = allStations.first()
        val defaults = getDefaultStations()
        for (defaultStation in defaults) {
            val exists = currentList.any { 
                it.name.lowercase() == defaultStation.name.lowercase() || 
                it.streamUrl == defaultStation.streamUrl 
            }
            if (!exists) {
                stationDao.insertStation(defaultStation)
            }
        }
    }

    private fun getDefaultStations(): List<Station> {
        return listOf(
            Station(
                name = "Хіт FM",
                streamUrl = "https://online.hitfm.ua/HitFM",
                iconUrl = "https://static.onlineradiobox.com/img/logo/8/7208.v15.png",
                genre = "Поп"
            ),
            Station(
                name = "KISS FM",
                streamUrl = "https://online.kissfm.ua/KissFM",
                iconUrl = "https://static.onlineradiobox.com/img/logo/9/7209.v21.png",
                genre = "Танцювальна"
            ),
            Station(
                name = "Radio ROKS",
                streamUrl = "https://online.radioroks.ua/RadioROKS",
                iconUrl = "https://static.onlineradiobox.com/img/logo/0/7210.v16.png",
                genre = "Рок"
            ),
            Station(
                name = "Люкс ФМ",
                streamUrl = "https://stream.lux.fm/lux/mp3/128/",
                iconUrl = "https://static.onlineradiobox.com/img/logo/3/13503.v14.png",
                genre = "Поп"
            ),
            Station(
                name = "Радіо Релакс",
                streamUrl = "https://online.radiorelax.ua/RadioRelax",
                iconUrl = "https://static.onlineradiobox.com/img/logo/1/7211.v15.png",
                genre = "Релакс"
            ),
            Station(
                name = "Радіо НВ",
                streamUrl = "https://online.radionv.ua/RadioNV",
                iconUrl = "https://static.onlineradiobox.com/img/logo/7/80047.v9.png",
                genre = "Новини/Розмови"
            ),
            Station(
                name = "Радіо Байрактар",
                streamUrl = "https://online.radiobayraktar.ua/RadioBayraktar",
                iconUrl = "https://static.onlineradiobox.com/img/logo/7/7207.v14.png",
                genre = "Українська"
            ),
            Station(
                name = "Наше Радіо",
                streamUrl = "https://online.nasheradio.ua/NasheRadio",
                iconUrl = "https://static.onlineradiobox.com/img/logo/3/7213.v13.png",
                genre = "Поп"
            ),
            Station(
                name = "Країна ФМ",
                streamUrl = "https://live.krayina.fm/krayina_fm",
                iconUrl = "https://static.onlineradiobox.com/img/logo/0/49930.v14.png",
                genre = "Українська"
            ),
            Station(
                name = "Радіо Промінь",
                streamUrl = "http://suspidcast.suspilne.media/ur2",
                iconUrl = "https://static.onlineradiobox.com/img/logo/5/13505.v10.png",
                genre = "Суспільне"
            ),
            Station(
                name = "Радіо Культура",
                streamUrl = "http://suspidcast.suspilne.media/ur3",
                iconUrl = "https://static.onlineradiobox.com/img/logo/1/70861.v10.png",
                genre = "Класика"
            ),
            Station(
                name = "Мелодія FM",
                streamUrl = "https://online.melodiafm.ua/MelodiaFM",
                iconUrl = "https://static.onlineradiobox.com/img/logo/2/7212.v13.png",
                genre = "Ретро"
            ),
            Station(
                name = "Радіо П'ятниця",
                streamUrl = "https://online.radiopyatnica.ua/RadioPyatnica",
                iconUrl = "https://static.onlineradiobox.com/img/logo/6/28156.v8.png",
                genre = "Поп"
            ),
            Station(
                name = "Радіо Джаз",
                streamUrl = "https://online.radiojazz.ua/RadioJazz",
                iconUrl = "https://static.onlineradiobox.com/img/logo/7/70857.v11.png",
                genre = "Джаз"
            ),
            Station(
                name = "DJFM",
                streamUrl = "http://91.228.140.3:8000/djfm_high",
                iconUrl = "https://static.onlineradiobox.com/img/logo/2/13502.v17.png",
                genre = "Танцювальна"
            ),
            Station(
                name = "Радіо Шансон",
                streamUrl = "https://online.shanson.ua/RadioShanson",
                iconUrl = "https://static.onlineradiobox.com/img/logo/4/7214.v14.png",
                genre = "Шансон"
            ),
            Station(
                name = "Lounge FM",
                streamUrl = "http://91.228.140.3:8000/loungefm_high",
                iconUrl = "https://static.onlineradiobox.com/img/logo/4/21804.v13.png",
                genre = "Релакс"
            ),
            Station(
                name = "Армія FM",
                streamUrl = "https://www.armyfm.com.ua:8000/stream",
                iconUrl = "https://static.onlineradiobox.com/img/logo/1/36101.v7.png",
                genre = "Новини/Розмови"
            ),
            Station(
                name = "Українське Радіо",
                streamUrl = "http://suspidcast.suspilne.media/ur1",
                iconUrl = "https://static.onlineradiobox.com/img/logo/4/13504.v11.png",
                genre = "Суспільне"
            ),
            Station(
                name = "Радіо Максимум",
                streamUrl = "https://stream.lux.fm/maximum/mp3/128/",
                iconUrl = "https://static.onlineradiobox.com/img/logo/3/25553.v14.png",
                genre = "Поп"
            ),
            Station(
                name = "Душевне Радіо",
                streamUrl = "http://strm.pepperfm.com.ua:8000/dushevnoe",
                iconUrl = "https://static.onlineradiobox.com/img/logo/2/31342.v12.png",
                genre = "Ретро"
            ),
            Station(
                name = "Nostalgie",
                streamUrl = "http://91.228.140.2:8000/nostalgie128",
                iconUrl = "https://static.onlineradiobox.com/img/logo/4/13504.v5.png",
                genre = "Ретро"
            ),
            Station(
                name = "Classic FM",
                streamUrl = "http://195.122.9.22:8000/classic",
                iconUrl = "https://static.onlineradiobox.com/img/logo/8/70858.v4.png",
                genre = "Класика"
            ),
            Station(
                name = "Super Radio",
                streamUrl = "http://superradio.com.ua:8000/128",
                iconUrl = "https://static.onlineradiobox.com/img/logo/9/13509.v15.png",
                genre = "Поп"
            ),
            Station(
                name = "Авторадіо",
                streamUrl = "https://online.avtoradio.ua/Avtoradio",
                iconUrl = "https://static.onlineradiobox.com/img/logo/6/13506.v14.png",
                genre = "Поп"
            ),
            Station(
                name = "Kiss FM Deep",
                streamUrl = "https://online.kissfm.ua/KissFM_deep",
                iconUrl = "https://static.onlineradiobox.com/img/logo/7/70867.v3.png",
                genre = "Танцювальна"
            ),
            Station(
                name = "Pepper FM",
                streamUrl = "http://strm.pepperfm.com.ua:8000/pepperfm",
                iconUrl = "https://static.onlineradiobox.com/img/logo/7/13507.v14.png",
                genre = "Поп"
            ),
            Station(
                name = "MFM Station",
                streamUrl = "http://91.228.140.2:8000/mfm128",
                iconUrl = "https://static.onlineradiobox.com/img/logo/6/13506.v22.png",
                genre = "Танцювальна"
            ),
            Station(
                name = "Львівська Хвиля",
                streamUrl = "http://onair.lviv.fm:8000/lviv.fm",
                iconUrl = "https://static.onlineradiobox.com/img/logo/0/13510.v16.png",
                genre = "Поп"
            ),
            Station(
                name = "Радіо Галичина",
                streamUrl = "http://91.124.7.133:8000/galychyna",
                iconUrl = "https://static.onlineradiobox.com/img/logo/1/13511.v15.png",
                genre = "Українська"
            ),
            Station(
                name = "Europa Plus",
                streamUrl = "http://91.228.140.2:8000/europaplus128",
                iconUrl = "https://static.onlineradiobox.com/img/logo/4/13514.v15.png",
                genre = "Поп"
            ),
            Station(
                name = "Jam FM",
                streamUrl = "https://online.jamfm.ua/JamFM",
                iconUrl = "https://static.onlineradiobox.com/img/logo/3/13513.v14.png",
                genre = "Рок"
            ),
            Station(
                name = "Radio ROKS - Свою Rock",
                streamUrl = "http://online.radioroks.ua/RadioROKS_ukr",
                iconUrl = "https://static.onlineradiobox.com/img/logo/5/38155.v8.png",
                genre = "Рок"
            ),
            Station(
                name = "Громадське Радіо",
                streamUrl = "https://stream.hromadskeradio.org/hromadskeradio.mp3",
                iconUrl = "https://static.onlineradiobox.com/img/logo/0/23610.v10.png",
                genre = "Новини/Розмови"
            )
        )
    }
}
