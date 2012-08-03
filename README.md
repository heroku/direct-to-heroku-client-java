A Java client for directly pushing artifacts Heroku via [Direct-to-Heroku](https://github.com/heroku/direct-to).

Setup
=====
Include as a dependency:

    <dependency>
        <groupId>com.herokuapp.directto</groupId>
        <artifactId>direct-to-heroku-client</artifactId>
        <version>${com.herokuapp.directto.version}</version>
    </dependency>

Basic Usage
===========
Example of pushing a war file:

    // Initialize the client
    DirectToHerokuClient client = new DirectToHerokuClient.Builder().setApiKey("your api key").build()

    // Prepare the payload
    Map<String, File> files = new HashMap<String, File>(1);
    files.put("war", new File(warFilePath));

    // Deploy!
    client.deploy("war", appName, files);

Advanced Usage
==============

Deploy Request
--------------
The basic usage example above works for standard deployments, but if more advanced deploy options are needed,
a `DeployRequest` builder object can also be passed to the `deploy()` method. For example:

    client.deploy(new DeployRequest(pipelineName, appName, files).setPollingTimeout(3000))

Event Subscription
------------------
This library does not log events directly; however, consumers can subscribe to events that occur during deployment
and perform any operation when notified of events. Event subscription is configured on the `DeployRequest` builder
by setting an `EventSubscription` with `Subscription`s for certain events. For example:

    new DeployRequest(pipelineName, appName, artifacts)
        .setEventSubscription(new EventSubscription()
                .subscribe(UPLOAD_START, new Subscriber() {
                    public void handle(Event event) {
                        // your action here
                    }
                })
        );

Consumer User Agent
-------------------
When using this library within a larger application, it is recommended to set the User Agent for the consumer application.
To do this, call `setConsumersUserAgent()` on `DirectToHerokuClient.Builder`. This will prepend the consumer user agent to
the full `User-Agent` header along with version information for this library in accordance to RFC 2616 Section 14.43.
For example, if you set `setConsumersUserAgent("SuperCoolCiSystem/1.0")`, the `User-Agent` header will be something like:

    User-Agent: SuperCoolCiSystem/1.0 direct-to-heroku-client/0.6-BETA-SNAPSHOT Java/1.6.0_33

Running Tests
=============
When running tests be sure to set system properties for test fixtures:
 * heroku.apiKey
 * heroku.appName

For example:

    mvn test -Dheroku.apiKey=1234 -Dheroku.appName=app-used-for-fixture

Heroku API JAR Cohabitation
===========================

When using this library along with the [Heroku API JAR](https://github.com/heroku/heroku.jar)
in the same project, it is recommended to use the `heroku-http-jersey-client` and `heroku-json-jersey-client`
providers instead of the standard Apache and Jackson providers:

    <dependency>
        <groupId>com.heroku.api</groupId>
        <artifactId>heroku-http-jersey-client</artifactId>
        <version>${com.heroku.api.version}</version>
    </dependency>
    <dependency>
        <groupId>com.heroku.api</groupId>
        <artifactId>heroku-json-jersey-client</artifactId>
        <version>${com.heroku.api.version}</version>
    </dependency>