require: requirements.sc

init:

    bind("postProcess", function($context) {
        $context.session.lastState = $jsapi.context().contextPath;
        if ($jsapi.context().currentState != '/noMatch') {
            $context.session.globalNoMatchCounter = 0;
        } else {
            $context.session.globalNoMatchCounter++;
        }
        if (!$jsapi.context().currentState.indexOf('/noMatch')) {
            $context.session.noMatchCounter = 0;
        } else {
            $context.session.noMatchCounter++;
        }
    });

    bind("onAnyError", function($context) {
        var answers = [
            "Что-то пошло не так. Нажмите /start.",
            "Произошла ошибка. Нажмите /start.",
            "Все сломалось. Попробуйте еще раз /start."
        ];
        var randomAnswer = answers[$reactions.random(answers.length)];
        $reactions.answer(randomAnswer);
        $reactions.buttons("/start")
    });

theme: /

    state: Start
        q!: $regex</start>
        script:
            $jsapi.startSession();
        go!: /Hello

    state: Hello
        if: $client.name
            script:
                $temp.name = ", " + $client.name
        a: Привет{{$temp.name}}. Я бот компании Just Tour. Я могу помочь узнать информацию о погоде, а так же оформить заявку на тур. Чем я могу Вам помочь?
        script: 
            $reactions.buttons(["Узнать погоду", "Оформить заявку на тур"]);

    state: Timedout
        script:
            timedout();