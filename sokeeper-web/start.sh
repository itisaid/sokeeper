export MAVEN_OPTS="-Xmx1700m"
nohup mvn jetty:run > log 2>&1 < /dev/null &
