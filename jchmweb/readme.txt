JChmLib

version: 0.3 (2006-09-17)

一、JChmLib 简介

JChmLib 是一个用来处理 Microsoft CHM 格式文件的 Java 库。它主要参考了 CHMLIB （详情参见 66.93.236.84/~jedwin/projects/chmlib/）。JChmLib 还很不成熟，不过已经可以处理大多数我所遇到的 .chm 文件，而且也支持中文、日文等。

JChmLib 附带了一个简单的 Http 服务器（org.jchmlib.net.ChmWeb），有了这个服务器，就可以通过 Web 浏览器来阅读 CHM 文件中的页面。由于 ChmWeb 并不是在启动的时候解压缩整个 Chm 文件，所以不用担心在打开大型文件时系统突然“假死”。 

ChmWeb 还实现了目录树与全文检索功能，更方便了阅读。

二、编译、安装

JChmLib 的构建使用 Ant，本目录下的 build.xml 是 Ant 构建项目时所用的脚本文件。在本目录下运行如下命令：
	ant all
即可完成编译、安装。

三、运行

下面介绍如何启动 ChmWeb。在 bin 目录下有两个脚本文件：jchmweb.bat 和 jchmweb.sh，分别用于 Windows 和 Linux 系统。

1. 环境配置：

运行 ChmWeb 需要 JRE 1.5，请到 http://java.sun.com 上下载。
修改脚本文件中的 JCHMLIB 变量，使其指向 jchmlib 目录。
为了运行的方便，可将 jchmlib/bin 路径加入到 PATH 中。

2. 启动服务器：

在命令行下敲入命令：
	jchmweb <port> <chmfile>
如：
	jchmweb.bat 8080 D:\somefile.chm
或
	jchmweb.sh 8080 /mnt/hda5/somefile.chm

稍等片刻，可看到如下提示
Server started. Now open your browser and type
         http://localhost:8080

现在，打开浏览器（如 Firefox），在地址栏敲入：
	http://localhost:8080
可以看到该 CHM 文件的根目录文件列表。

如果你的浏览器支持框架（一般都支持吧，除了 Linux 下文本模式的浏览器 Lynx 等），可用这个地址：
http://localhost:8080/@index.html
左边的框架中会显示目录树，右边的框架会显示 Chm 文件的首页（起始页面）。

希望你能到论坛（http://gro.clinux.org/forum/?group_id=886）发发牢骚，提提意见。也可以发邮件给我：

Chimen Chen
chimenchen@gmail.com

