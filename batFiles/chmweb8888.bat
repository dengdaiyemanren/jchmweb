start java -classpath .;jchmweb2.jar -Djchmweb.template=%JCHMLIB%/bin org.jchmlib.net.ChmWeb 8888 %1
start firefox http://localhost:8888/@index.html
