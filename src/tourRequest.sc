theme: /TourRequest

    state: TourRequest
        intent!: /TourRequest
        script:
            $session.tourRequest = true;
            cityOrCountry();
            timeFormation();
            $session.startdateEmail = $parseTree._Date.value;
            $temp.now = new Date();
            $temp.date = new Date($session.date);
            if ($temp.date > $temp.now) {
                $temp.day = $temp.date.getDate();
                if ($parseTree._monthPart) {
                     $temp.day = $parseTree._monthPart.value;
                }
            }
        if: $session.destination
            go!: /TourTheme/TourRequestFill
        else:
            go!: /TourTheme/SpecifiedCity

    state: SpecifiedCity
        random:
            a: Уточните город или страну.
            a: В какой город или страну планируете поездку?

        state: HaveCity
            intent: /City
            script: 
                cityOrCountry();
            go!: /TourTheme/TourRequestFill

        state: noMatch
            event: noMatch
            script: 
                $session.destination = "-";
            go!: /TourTheme/HelpManager
    
    state: HelpManager
        random:
            a: Выбрать город поможет менеджер.
            a: Менеджер тур агентства поможет определиться с городом.
            
        go!: /TourTheme/TourRequestFill
        
    state: TourRequestFill
        random:
            a: Для оформления заявки необходимо ответить на несколько вопросов.
            a: Чтобы оформить заявку, ответьте на несколько вопросов.
        go!: /TourTheme/TourRequestFill/HaveNameNumber
        
        state: HaveNameNumber
            if: $client.name && $client.phoneNumber // все данные есть, проверяем их
                go!: /TourTheme/TourRequestFill/CheckNameNumber
            elseif: $client.name // есть имя - спрашиваем номер
                go!: /TourTheme/TourRequestFill/ChangeNameNumber/OnlyNumber
            elseif: $client.phoneNumber // есть номер, нет имени
                go!: /TourTheme/TourRequestFill/ChangeNameNumber/OnlyName");
            else:
                go!: /TourTheme/TourRequestFill/NameNumber

        state: CheckNameNumber
            script:
                $client.name = capitalize($client.name);
            a: Ваше имя: {{$client.name}}. Ваш номер телефона: {{$client.phoneNumber}}. Все верно?
            
            state: Yes
                q: $yes
                q: * (да|даа|lf|ага|точно|угу|верно|ок|ok|окей|окай|okay|именно|подтвержд*|йес|планир*|хочу|хот|*соглас*|норм*|конечно|верн*|так) *
                go!: /TourTheme/TourRequestFill/NumberPeople

            state: No
                q: $no
                q: * (нет/не верн*/помен*/изменит*/друг*/не правильн*) *
                go!: /TourTheme/TourRequestFill/ChangeNameNumber
            
            state: noMatch
                event: noMatch
                if: $session.noMatchCounter > 1
                    a: Сожалею, при оформлении заявки произошла ошибка.
                    go!: /ExcessLocalNoMatch
                else:
                    a: Я не могу оформить заявку без контактных данных. Действительно ли Вас зовут {{$client.name}}, а Ваш номер телефона {{$client.phoneNumber}}?

        state: NameNumber
            random:
                a: Представьтесь, пожалуйста.
                a: Укажите Ваше имя.
                a: Как Вас зовут?
            go!: /TourTheme/TourRequestFill/NameNumber/GetName

            state: GetName
               
                state: HaveName
                    q: * @UserNames::name *
                    q: * @pymorphy.name::pymorphyName *
                    script: 
                        if ($parseTree._name) {
                            $client.name = capitalize($parseTree._name.full);
                        } else if ($parseTree._pymorphyName) {
                            $client.name = capitalize($parseTree._pymorphyName);
                        }
                        if ($client.name) {
                            // имя есть, запрашиваем номер
                            $reactions.transition("/TourTheme/TourRequestFill/ChangeNameNumber/OnlyNumber");
                        } else {
                            $reactions.answer("Мне не удалось определить имя. Пожалуйста, укажите Ваше полное имя.");
                            $reactions.transition("/TourTheme/TourRequestFill/NameNumber/GetName");
                        }

                state: noMatch
                    event: noMatch
                    script:
                        $client.name = titleCase(String($request.query));
                        $reactions.transition("/TourTheme/TourRequestFill/ChangeNameNumber/OnlyNumber");

        state: ChangeNameNumber
            a: Укажите, пожалуйста, Ваше имя и номер телефона.
            buttons:
                "Исправить имя" -> ./OnlyName
                "Исправить номер телефона" -> ./OnlyNumber
            
            state: HaveNameNumber
                q: * { @UserNames::name * @duckling.phone-number::phone } *
                q: * { @pymorphy.name::pymorphyName * @duckling.phone-number::phone }
                q: * @pymorphy.name::pymorphyName *
                q: * @UserNames::name *
                q: * @duckling.phone-number::phone *
                script:
                    //валидируем указанные данные
                    if ($parseTree._name) {
                        $client.name = capitalize($parseTree._name.full);
                    } else if ($parseTree._pymorphyName) {
                        $client.name = capitalize($parseTree._pymorphyName);
                    }
                    if ($parseTree._phone) {
                        $temp.phoneNumber = validatePhoneNumber(String($parseTree._phone));
                    }
                    if (($parseTree._name||$parseTree._pymorphyName) && $parseTree._phone) {
                        // были введены все новые данные
                        $client.phoneNumber = $temp.phoneNumber;
                        $reactions.transition("/TourTheme/TourRequestFill/NumberPeople");
                    } else if ($parseTree._phone && $client.name) {
                        $client.phoneNumber = $temp.phoneNumber;
                        $reactions.transition("/TourTheme/TourRequestFill/NumberPeople");
                    } else if ($client.name) {
                        // есть только имя 
                        if ($temp.phoneNumber === false) {
                            // был указан не валидный номер телефона
                            $reactions.answer("Указанный Вами номер не валидный.");
                        }
                        $reactions.transition("/TourTheme/TourRequestFill/ChangeNameNumber/OnlyNumber");
                    } else if ($temp.phoneNumber) {
                        // есть только номер, нет имени
                        $client.phoneNumber = $temp.phoneNumber;
                        if ($session.dontAskOther) {
                            // имя уже было исправлено ранее
                            $reactions.transition("/TourTheme/TourRequestFill/NumberPeople");
                        }
                        // нет имени, не распознанное имя
                        $reactions.transition("/TourTheme/TourRequestFill/ChangeNameNumber/OnlyName");
                    } else {
                        // не валидный номер телефона
                        $reactions.answer("Указанный Вами номер не валидный.");
                        $reactions.transition("/TourTheme/TourRequestFill/ChangeNameNumber/OnlyNumber");
                    }

            state: noMatch
                event: noMatch
                script:
                    if ($session.noMatchCounter > 1) {
                        $reactions.answer("Я не могу оформить заявку без Вашего имени и номера телефона.");
                        $reactions.transition("/ExcessLocalNoMatch");
                    } else {
                        $reactions.answer("Для оформления заявки имя и номер телефона обязательны. Пожалуйста, напишите Ваше имя.");
                    }    
                    
            state: OnlyNumber
                random:
                    a: Укажите Ваш полный номер телефона
                    a: Напишите Ваш номер телефона
                    a: Какой у Вас номер телефона?
                
                state: HaveNameNumber
                    q: * @duckling.phone-number::phone *
                    script: 
                        $client.phoneNumber = validatePhoneNumber(String($parseTree._phone));
                        if ($client.phoneNumber) {
                            $reactions.transition("/TourTheme/TourRequestFill/NumberPeople");
                        } else {
                            $reactions.answer("Номер должен содержать 11 символов.");
                            $reactions.transition("/TourTheme/TourRequestFill/ChangeNameNumber/OnlyNumber/noMatch");
                        }
            
                state: noMatch
                    event: noMatch
                    script:
                        if ($session.noMatchCounter > 1) {
                            $reactions.answer("Я не могу оформить заявку без Вашего номера телефона.");
                            $reactions.transition("/ExcessLocalNoMatch");
                        } else {
                            $reactions.answer("Для оформления заявки, имя и номер телефона обязательны. Напишите Ваш полный номер телефона.");
                        }
                        
            state: OnlyName
                random:
                    a: Представьтесь, пожалуйста
                    a: Укажите Ваше имя
                    a: Как Вас зовут?
                script:
                    $reactions.transition("./GetName");

                state: GetName

                    state: HaveName
                        q: * @UserNames::name *
                        q: * @pymorphy.name::pymorphyName *
                        script: 
                            if ($parseTree._name) {
                                $client.name = capitalize($parseTree._name.full);
                            } else if ($parseTree._pymorphyName) {
                                $client.name = capitalize($parseTree._pymorphyName);
                            }
                            if ($client.name) {
                                $reactions.transition("/TourTheme/TourRequestFill/NumberPeople");
                            } else {
                                $reactions.transition("/TourTheme/TourRequestFill/ChangeNameNumber/OnlyName/GetName/noMatch");
                            }

                    state: noMatch
                        event: noMatch
                        script:
                            if ($session.noMatchCounter > 3) {
                                    $reactions.answer("Я не могу оформить заявку без Вашего имени.");
                                    $reactions.transition("/ExcessLocalNoMatch");
                            } else {
                                $session.specName = $request.query;
                                $reactions.answer("Уточняю, Ваше имя "+ $session.specName + "?");
                                $reactions.transition("/TourTheme/TourRequestFill/ChangeNameNumber/OnlyName/GetName/SpecifyName");
                            }

                    state: SpecifyName

                        state: Yes
                            q: $yes
                            script:
                                $client.name = titleCase(String($session.specName));
                                $session.specName = undefined;
                                $reactions.transition("/TourTheme/TourRequestFill/NumberPeople");

                        state: No
                            q: $no
                            script:
                                $reactions.answer("Пожалуйста, напишите Ваше полное имя.");
                                $reactions.transition("/TourTheme/TourRequestFill/ChangeNameNumber/OnlyName/GetName");

                        state: noMatch
                            event: noMatch
                            script:
                                // не удалось уточнить имя
                                if ($session.noMatchCounter > 2) {
                                    $reactions.answer("К сожалению, я всё равно не могу определить Ваше имя.");
                                    $reactions.transition("/ExcessLocalNoMatch");
                                }
                                $session.noMatchName = $request.query;
                                $reactions.transition("/TourTheme/TourRequestFill/ChangeNameNumber/OnlyName/GetName");

        state: NumberPeople
            random:
                a: Уточните, сколько взрослых поедет в тур?
                a: Сколько взрослых людей поедут в тур?
                a: Какое количество взрослых планируют поехать?
            buttons:
                "Пропустить" -> ./SkipQuestion

            state: HaveNumberPeople
                q: * @duckling.number *
                script: 
                    $session.numberPeople = $entities[0].value;
                    if ($session.numberPeople < 1) {
                        if ($session.noMatchCounter > 1) {
                            $reactions.answer("Я не могу оформить заявку без взрослых.");
                            $reactions.transition("/ExcessLocalNoMatch");
                        } else {
                            $reactions.answer("Укажите, пожалуйста, число взрослых людей, которые планируют отправиться в тур.");
                        }
                    } else {
                        $reactions.transition("/TourTheme/TourRequestFill/NumberKids");
                    }

            state: InaccurateAnswer
                q: * { [взросл*] [только] я [и|с] [друз*|мам*|пап*|брат*|сестр*|муж*|жен*|доч*|сын*|родител*|дет*] } *
                script:
                    $session.numberPeople = $request.query;
                    $reactions.transition("/TourTheme/TourRequestFill/NumberKids");
                    
            state: SkipQuestion
                q: $doubt
                script:
                    $session.numberPeople = $request.query;
                    if ($request.query === "Пропустить") {
                        $session.numberPeople = "-";
                    }
                    $reactions.transition("/TourTheme/TourRequestFill/NumberKids");

            state: noMatch
                event: noMatch
                script:
                    if ($session.noMatchCounter > 1) {
                        $session.numberPeople = $request.query;
                        $reactions.transition("/TourTheme/TourRequestFill/NumberKids");
                    } else {
                        $reactions.answer("Укажите, пожалуйста, число взрослых людей, которые планируют отправиться в тур.");
                    }
                buttons:
                    "Пропустить" -> /TourTheme/TourRequestFill/NumberPeople/SkipQuestion

        state: NumberKids
            random:
                a: Уточните количество детей в поездке.
                a: Сколько будет детей?
            buttons:
                "Пропустить" -> ./SkipQuestion
            
            state: HaveNumberKids
                q: * @duckling.number *
                script: 
                    $session.numberKids = $entities[0].value;
                go!: /TourTheme/TourRequestFill/Budget
                
            state: HaveMultipleNumberKids
                q: * @duckling.number::x * @duckling.number::y *
                q: * {@duckling.number::x * (мальчик/девочка/пацан/девчонка/девчёнка/девченка)} *
                q: * (мальчик/девочка/пацан/девчонка/девчёнка/девченка) * (мальчик/девочка/пацан/девчонка/девчёнка/девченка) *
                script:
                    $temp._x = Number($parseTree._x);
                    $temp._y = Number($parseTree._y);
                    if (isNaN($parseTree._x)) {
                        $temp._x = 0;
                    }
                    if (isNaN($parseTree._y)) {
                       $temp._y = 0;
                    }
                    
                    if ($temp._y === 0) {
                        if ($temp._x === 0) {
                            $session.numberKids = 2;
                        } else {
                            $session.numberKids = 1 + $temp._x;
                        } 
                    } else {
                        $session.numberKids = $temp._x + $temp._y
                    }
                go!: /TourTheme/TourRequestFill/Budget
            
            state: HaveZeroKids
                q: * { (нет*/без/не буд*/не поед*) * [детей/ребен*/дети/них/него/его/ее/их] } *
                q: * { (только) * [я|мы|взрослые] } *
                script: 
                    $session.numberKids = 0;
                go!: /TourTheme/TourRequestFill/Budget

            state: noMatch
                event: noMatch
                script:
                    if ($session.noMatchCounter > 1) {
                        $session.numberKids = $request.query;
                        $reactions.transition("/TourTheme/TourRequestFill/Budget");
                    } else {
                        $reactions.answer("Укажите, пожалуйста, число детей, которые планируют отправиться в тур.");
                    }
                buttons:
                    "Пропустить" -> /TourTheme/TourRequestFill/NumberKids/SkipQuestion

            state: SkipQuestion
                q: $doubt
                script:
                    $session.numberKids = "-";
                    $reactions.transition("/TourTheme/TourRequestFill/Budget");

        state: Budget
            random:
                a: Укажите ориентировочный бюджет поездки?
                a: В какую сумму планируете уложиться?
                a: Какую сумму Вы планируете выделить на поездку?
            buttons:
                "Пропустить" -> ./SkipQuestion
                
            state: HaveBudget
                q: * @duckling.number::budget *
                script: 
                    $session.budget = Math.abs($parseTree._budget); 
                go!: /TourTheme/TourRequestFill/StarRating

            state: SkipQuestion
                q: $doubt
                script:
                    $session.budget = $request.query;
                    if ($request.query === "Пропустить") { 
                        $session.budget = "-";
                    }
                    $reactions.transition("/TourTheme/TourRequestFill/StarRating");

            state: noMatch
                event: noMatch
                script: 
                    if ($session.noMatchCounter > 1) {
                        $session.budget = $request.query;
                        $reactions.transition("/TourTheme/TourRequestFill/StarRating");
                    } else {
                        $reactions.answer("Укажите, пожалуйста, примерный бюджет.");
                    }
                buttons:
                    "Пропустить" -> /TourTheme/TourRequestFill/Budget/SkipQuestion

        state: StarRating
            random:
                a: Укажите желаемую "звёздность" отеля?
                a: Какие предпочтения по "звёздности" отеля?
            buttons:
                "Пропустить" -> ./SkipQuestion
                
            state: HaveStarRating
                q: * @duckling.number *
                q: * @UserStarRating::rating *
                script:
                    if ($parseTree._rating) {
                        $session.starRating = $parseTree._rating.value;
                    } else {
                        $session.starRating = $entities[0].value;
                    }
                    if ($session.starRating > 0 && $session.starRating < 6) {
                        $reactions.transition('/TourTheme/TourRequestFill/StartDate');
                    } else if ($session.noMatchCounter > 1) {
                        $session.starRating = $request.query;
                        $reactions.transition("/TourTheme/TourRequestFill/StartDate");
                    } else {
                        $reactions.answer("В нашей базе есть отели только от 1 до 5 звёзд.");
                        $reactions.answer("Укажите, пожалуйста, желаемую звёздность отеля цифрой.");
                    }

            state: SkipQuestion
                q: * { (нет|нет*|без|никаких|никакой|не зна*|не решил*|любой|любая) * [предпочтен*|разницы] } *
                q: $doubt
                script:
                    $session.starRating = "-";
                    $reactions.transition("/TourTheme/TourRequestFill/StartDate");

            state: noMatch
                event: noMatch
                script: 
                    if ($session.noMatchCounter > 1) {
                        $session.starRating = $request.query;
                        $reactions.transition("/TourTheme/TourRequestFill/StartDate");
                    } else {
                        $reactions.answer("Укажите, пожалуйста, желаемую звёздность отеля цифрой.");
                    }
                buttons:
                    "Пропустить" -> /TourTheme/TourRequestFill/StarRating/SkipQuestion

        state: StartDate
            script:
                if ($session.date) {
                    $temp.now = new Date();
                    $temp.date = new Date($session.date);
                    if ($temp.date > $temp.now) {
                        $temp.day = $temp.date.getDate();
                        if ($parseTree._monthPart) {
                            $temp.day = $parseTree._monthPart.value;
                        }
                        $session.startdate = stringifyDate($temp.date.getFullYear(), $temp.date.getMonth()+1, $temp.day);
                        $reactions.transition('/TourTheme/TourRequestFill/Duration');
                    } else {
                        $reactions.answer("Вы указали прошедшую дату. Укажите, пожалуйста, дату начала поездки.");
                    }
                } else {
                    $reactions.answer("Укажите дату начала поездки.");
                    $reactions.buttons({text: "Пропустить", transition: "/TourTheme/TourRequestFill/StartDate/SkipQuestion"})
                }

            state: HaveStartDate
                intent: /Дата
                script:
                    $session.date = $parseTree._Date.value.slice(0,10);
                    $session.startdateEmail = $parseTree._Date.value;
                    $temp.now = new Date();
                    $temp.date = new Date($session.date);
                    if ($temp.date > $temp.now) {
                        $temp.day = $temp.date.getDate();
                        if ($parseTree._monthPart) {
                             $temp.day = $parseTree._monthPart.value;
                        }
                        $session.startdate = stringifyDate($temp.date.getFullYear(), $temp.date.getMonth()+1, $temp.day);
                        $reactions.transition('/TourTheme/TourRequestFill/Duration');
                    } else {
                        $reactions.answer("Вы указали прошедшую дату. Укажите, пожалуйста, дату начала поездки.");
                    }    
            
            state: noMatch
                event: noMatch
                script: 
                    if ($session.noMatchCounter > 1) {
                        $session.startdate = $request.query;
                        $reactions.transition("/TourTheme/TourRequestFill/Duration");
                    } else {
                        $reactions.answer("Укажите, пожалуйста, примерную дату начала поездки.");
                    }
                buttons:
                    "Пропустить" -> /TourTheme/TourRequestFill/StartDate/SkipQuestion

            state: SkipQuestion
                q: $doubt
                script:
                    $session.startdate = $request.query;
                    if ($request.query === "Пропустить") {
                        $session.startdate = "-";
                    }
                    $reactions.transition("/TourTheme/TourRequestFill/Duration");

        state: Duration
            random:
                a: Укажите длительность поездки?
                a: На какой срок планируете поехать?
            buttons:
                "Пропустить" -> ./SkipQuestion

            state: HaveDuration
                intent: /Длительность 
                script: 
                    $session.duration = $parseTree._duration.value;
                    if ($parseTree._amount !== undefined && $parseTree._amount != $parseTree._duration.value) {
                        $session.duration = $session.duration * $parseTree._amount;
                    }
                    if ($parseTree._duration.unit === "week") {
                        $session.duration = $session.duration * 7;
                    }
                    if ($parseTree._duration.unit === "month") {
                        $session.duration = $session.duration * 30;
                    }
                    if ($parseTree._duration.unit === "year") {
                        $session.duration = $session.duration * 365;
                    }

                go!: /TourTheme/TourRequestFill/Comment

            state: UserDuration
                q: * @UserDuration::duration *
                q: * { @duckling.number::amount * @UserDuration::duration } *
                script: 
                    $session.durationStruct = $parseTree._duration;
                    if ($parseTree._duration) {
                        $session.duration = $session.durationStruct.value; 
                        if ($parseTree._amount) {
                            $session.duration = $parseTree._amount * $session.durationStruct.value;
                        }
                    }
                go!: /TourTheme/TourRequestFill/Comment

            state: noMatch
                event: noMatch
                script: 
                    if ($session.noMatchCounter > 1) {
                        $session.duration = $request.query;
                        $reactions.transition("/TourTheme/TourRequestFill/Comment");
                    } else {
                        $reactions.answer("Укажите, пожалуйста, примерную длительность поездки.");
                    }
                buttons:
                    "Пропустить" -> /TourTheme/TourRequestFill/Duration/SkipQuestion

            state: SkipQuestion
                q: $doubt
                script:
                    $session.duration =$request.query;
                    if ($request.query === "Пропустить") {
                        $session.duration = "-";
                    }
                    $reactions.transition("/TourTheme/TourRequestFill/Comment");

        state: Comment
            a: Вы можете оставить комментарий для менеджера в свободной форме.
            buttons:
                "Пропустить" -> ./SkipQuestion
                
            state: HaveComment
                q: *
                script:
                    $session.comment = $request.query;
                go!: /Confirmation

            state: SkipQuestion
                script:
                    $session.comment = "-";
                    $reactions.transition("/Confirmation");

