JChmLib

version: 0.3 (2006-09-17)

һ��JChmLib ���

JChmLib ��һ���������� Microsoft CHM ��ʽ�ļ��� Java �⡣����Ҫ�ο��� CHMLIB ������μ� 66.93.236.84/~jedwin/projects/chmlib/����JChmLib ���ܲ����죬�����Ѿ����Դ����������������� .chm �ļ�������Ҳ֧�����ġ����ĵȡ�

JChmLib ������һ���򵥵� Http ��������org.jchmlib.net.ChmWeb��������������������Ϳ���ͨ�� Web ��������Ķ� CHM �ļ��е�ҳ�档���� ChmWeb ��������������ʱ���ѹ������ Chm �ļ������Բ��õ����ڴ򿪴����ļ�ʱϵͳͻȻ���������� 

ChmWeb ��ʵ����Ŀ¼����ȫ�ļ������ܣ����������Ķ���

�������롢��װ

JChmLib �Ĺ���ʹ�� Ant����Ŀ¼�µ� build.xml �� Ant ������Ŀʱ���õĽű��ļ����ڱ�Ŀ¼�������������
	ant all
������ɱ��롢��װ��

��������

�������������� ChmWeb���� bin Ŀ¼���������ű��ļ���jchmweb.bat �� jchmweb.sh���ֱ����� Windows �� Linux ϵͳ��

1. �������ã�

���� ChmWeb ��Ҫ JRE 1.5���뵽 http://java.sun.com �����ء�
�޸Ľű��ļ��е� JCHMLIB ������ʹ��ָ�� jchmlib Ŀ¼��
Ϊ�����еķ��㣬�ɽ� jchmlib/bin ·�����뵽 PATH �С�

2. ������������

�����������������
	jchmweb <port> <chmfile>
�磺
	jchmweb.bat 8080 D:\somefile.chm
��
	jchmweb.sh 8080 /mnt/hda5/somefile.chm

�Ե�Ƭ�̣��ɿ���������ʾ
Server started. Now open your browser and type
         http://localhost:8080

���ڣ������������ Firefox�����ڵ�ַ�����룺
	http://localhost:8080
���Կ����� CHM �ļ��ĸ�Ŀ¼�ļ��б�

�����������֧�ֿ�ܣ�һ�㶼֧�ְɣ����� Linux ���ı�ģʽ������� Lynx �ȣ������������ַ��
http://localhost:8080/@index.html
��ߵĿ���л���ʾĿ¼�����ұߵĿ�ܻ���ʾ Chm �ļ�����ҳ����ʼҳ�棩��

ϣ�����ܵ���̳��http://gro.clinux.org/forum/?group_id=886��������ɧ�����������Ҳ���Է��ʼ����ң�

Chimen Chen
chimenchen@gmail.com

