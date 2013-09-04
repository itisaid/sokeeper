sokeeper
========

for fun project

run the web app on https://console.appfog.com/

1> under sokeeper: mvn clean install eclipse:eclipse -DdownloadSources -Dmaven.test.skip
2> under sokeeper-web/target/sokeeper-web-1.0.0-SNAPSHOT: 
    af login 
    af update the_appfoge_app_name(e.g.:sokeeper), make sure allocate at least 2GB mem for your appfoge app
3> visit http://sokeeper.aws.af.cm

                         