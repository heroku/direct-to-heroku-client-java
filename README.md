A Java client for directly pushing artifacts Heroku via [Direct-to-Heroku](https://github.com/heroku/direct-to).

Setup
-----
Include as a dependency:

    <dependency>
        <groupId>com.herokuapp.directto</groupId>
        <artifactId>direct-to-heroku-client</artifactId>
        <version>0.4-SNAPSHOT</version>
    </dependency>
Usage
-----
Example of pushing a war file:

    // Initialize the client
    DirectToHerokuClient client = new DirectToHerokuClient("your api key");

    // Prepare the payload
    Map<String, File> files = new HashMap<String, File>(1);
    files.put("war", new File(warFilePath));

    // Deploy!
    client.deploy("war", appName, files);


Running Tests
-------------
When running tests be sure to set system properties for test fixtures:
 * heroku.apiKey
 * heroku.appName

For example:

    mvn test -Dheroku.apiKey=1234 -Dheroku.appName=app-used-for-fixture

Heroku API JAR Cohabitation
---------------------------

If you are using this JAR along with the [Heroku API JAR](https://github.com/heroku/heroku.jar)
in the same project, you will need to explictly declare Jackson version 1.8.3 in `dependencyManagement`
section of your project's POM:

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.codehaus.jackson</groupId>
                <artifactId>jackson-mapper-asl</artifactId>
                <version>1.8.3</version>
            </dependency>
            <dependency>
                <groupId>org.codehaus.jackson</groupId>
                <artifactId>jackson-core-asl</artifactId>
                <version>1.8.3</version>
            </dependency>
        </dependencies>
    </dependencyManagement>