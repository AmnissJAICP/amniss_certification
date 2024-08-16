function testMode() {
    return $.request.channelType === 'chatwidget';
}

function cityOrCountry() {
    if ($parseTree._city) {
        $.session.destination = capitalize($parseTree._city.name);
        $.session.destinationType = "город";
    }
    if ($parseTree._country) {
        $.session.destination = capitalize($parseTree._country.name);
        $.session.destinationType = "страну";
    }
}

function cityFormation() {
    if ($parseTree._city) {
        $.session.destination = capitalize($parseTree._city.name);
        $.session.destinationType = "город";
    }
}

function timeFormation() {
    if ($parseTree._date) {
        $.session.date = $parseTree._date.value.slice(0,10);
    }
    if ($parseTree._Date) {
        $.session.date = $parseTree._Date.value.slice(0,10);
    }
    if ($parseTree._Time) {
        $.session.date = $parseTree._Time.value.slice(0,10);
    }
}

function extractValues() {
    var extractValuesEntities = $caila.entitiesLookup($request.query, true)
    if (extractValuesEntities || Array.isArray(extractValuesEntities.entities)) {
        if (extractValuesEntities.entities.entity === "UserCountries") {
            $.temp.counrtyChoice = true;
        } else {
            $.session.destination = capitalize($parseTree._city.name);
            $.session.destinationType = "город";
        }
    }
}

function weatherRequest() {
    var city = $caila.inflect($session.destination, ["nomn"]);
    var response = openWeatherMapCurrent("metric", "ru", city)
    if (response && response.main && response.isOk) {
        $.session.temperature = Math.round(response.main.temp); 
        return true;
    }
    return false;
}

function timedout() {
    $.client.lastState = $.context().contextPath;
    $.client.arrayClientData = $.session
}