set JCHMLIB=D:/bin/jchmlib
set CLASSPATH=%CLASSPATH%;%JCHMLIB%/bin/jchmlib.jar
start java -Djchmweb.template=%JCHMLIB%/bin org.jchmlib.net.ChmWeb 8888 %1
start firefox http://localhost:8888/@index.html
