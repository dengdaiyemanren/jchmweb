set JCHMLIB=D:/bin/jchmlib
set CLASSPATH=%CLASSPATH%;%JCHMLIB%/bin/jchmlib.jar
start java -Djchmweb.template=%JCHMLIB%/bin com.google.code.jchmweb.jchmweb2.net.ChmWeb 8088 %1
start firefox http://localhost:8888/@index.html
