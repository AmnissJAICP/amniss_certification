theme: /WeatherTheme

    state: Weather || noContext = true
        intent!: /Weather_Request
        q!: * $weather *
        script:
            extractValues(); //определяем выбрал клиент страну или город
            timeFormation(); //формируем время и дату для ответа клиенту
        go!: /WeatherTheme/Weather/Allocation
        
        state: Allocation
            if: $session.date === "-" || $session.date === undefined
                if: ($session.destination === "-" || $session.destination === undefined) && $temp.counrtyChoice
                    go!: /WeatherTheme/Weather/NoCityNoDate
                else: 
                    go!: /WeatherTheme/Weather/NoDate
            else: 
                if: ($session.destination === "-" || $session.destination === undefined) && $temp.counrtyChoice
                    go!: /WeatherTheme/Weather/NoCity
                else:
                    go!: /WeatherTheme/WeatherRequest

        state: NoCityNoDate
            random:
                a: Уточните в каком городе и на какую дату хотите узнать погоду?
                a: В каком городе и на какую дату сообщить Вам погоду?
            
            state: HaveCityDate
                intent: /Погода
                script:
                    cityFormation();
                    timeFormation();
                go!: /WeatherTheme/WeatherRequest
                
            state: noMatch
                event: noMatch
                if: $session.noMatchCounter > 1
                    a: К сожалению, я не смог узнать погоду.
                    go!: /ExcessLocalNoMatch
                else:
                    a: Чтобы узнать погоду, напишите город и дату.

        state: NoDate
            random:
                a: Уточните, на какую дату Вас интересует погода?
                a: На какую дату Вас интересует погода?
            
            state: HaveDate
                intent: /Дата
                script:
                    timeFormation();
                go!: /WeatherTheme/WeatherRequest

            state: noMatch
                event: noMatch
                if: $session.noMatchCounter > 1
                    a: К сожалению, я не смог узнать погоду.
                    go!: /ExcessLocalNoMatch
                else:
                    a: Чтобы узнать погоду, укажите интересующую Вас дату.

        state: NoCity
            random:
                a: Уточните, в каком городе Вас интересует погода?
                a: В каком городе Вы хотите узнать погоду?
                
            state: HaveCity
                intent: /Город
                script:
                    cityFormation();
                go!: /WeatherTheme/WeatherRequest
                
            state: noMatch
                event: noMatch
                if: $session.noMatchCounter > 1
                    a: К сожалению, я не смог узнать погоду.
                    go!: /ExcessLocalNoMatch
                else:
                    a: Чтобы узнать погоду, укажите интересующий Вас город.

    state: WeatherRequest
        script:
        $temp.resIsOk = weatherRequest();    
        if: $temp.resIsOk
            a: Погода в {{capitalize($caila.inflect($session.destination, ["loct"]))}} на {{ $session.date }}: {{$session.temperature}} °C
            if: $session.temperature < 0
                a: Вы планируете поездку в {{$session.destinationType}} с холодным климатом?
            elseif: $session.temperature > 20
                a: Вы планируете поездку в {{$session.destinationType}} с жарким климатом?
            else:
                a: Вы планируете поездку в {{$session.destinationType}} с умеренным климатом?"
            script:
                $reactions.buttons(["Да", "Нет"]);
                $reactions.transition("/WeatherTheme/WeatherRequest/Process"); 
        else:
            a: Что-то сервер барахлит. Не могу узнать погоду.
            go!: /WeatherTheme/WeatherRequest/ProcessError

        state: ProcessError
            if: $session.tourRequest
                a: Хотите продолжить оформление тура в {{capitalize($caila.inflect($session.destination , ["accs"]))}} без погодных данных?
            else:
                a: Хотите оформить тур в {{capitalize($caila.inflect($session.destination , ["accs"]))}} без погодных данных?
            script:
                $reactions.buttons(["Да", "Нет"])

            state: Yes
                q: $yes
                go!: /TourTheme/TourRequestFill
            
            state: No
                q: $no
                random:
                    a: Тогда Вы можете проверить погоду на любом сайте прогноза погоды, например www.gismeteo.ru, и вернуться для оформления заявки. 
                    a: В таком случае, предлагаю Вам проверить погоду на любом сайте прогноза погоды, а затем здесь оформить заявку на тур.  
                go!: /Bye

            state: noMatch
                event: noMatch
                if: $session.noMatchCounter > 1
                    a: Меня еще не научили понимать это, но я передам своему создателю, чтобы он меня научил. Оформить заявку на тур Вы также можете по телефону 88120000000.
                    go!: /Bye
                else:
                    a: Простите, не понял. Хотите ли перейти к оформлению тура без данных о погоде?

        state: Process

            state: ChangeDestination
                intent: /Город
                script: 
                    cityFormation();
                go!: /WeatherTheme/WeatherRequest
            
            state: ChangeDate
                intent: /Дата
                script:
                    timeFormation()
                go!: /WeatherTheme/WeatherRequest

            state: Yes
                q: ($yes/$comYes)
                q: 
                if: $session.tourRequest
                    a: Хотите продолжить оформление тура в {{capitalize($caila.inflect($session.destination , ["accs"]))}}?
                else:
                    a: Хотите оформить тур в {{capitalize($caila.inflect($session.destination , ["accs"]))}}?
                go!: /WeatherTheme/WeatherRequest/Tour

            state: No
                q: $no
                random:
                    a: Хотите узнать погоду в другой стране или городе?
                    a: Узнаем погоду в другом городе или стране?
                go!: /WeatherTheme/WeatherRequest/ChangeDestination  

            state: noMatch
                event: noMatch
                if: $session.noMatchCounter > 1
                    a: Пока меня еще не научили понимать это, но я передам создателю, чтобы он меня научил.
                    go!: /ExcessLocalNoMatch
                else:
                    a: Уточните, действительно ли Вы планируете поездку в страну с таким климатом?
                script:
                    $reactions.timeout({interval: $injector.timeoutTime, targetState: "/Timedout"})

