function openWeatherMapCurrent(units, lang, city){
    return $http.get("http://api.openweathermap.org/data/2.5/weather?APPID=${APPID}&units=${units}&lang=${lang}&q=${q}", {
            timeout: 10000,
            query:{
                APPID: OPENWEATHERMAP_API_KEY,
                units: units,
                lang: lang,
                q: city
            }
        });
    }