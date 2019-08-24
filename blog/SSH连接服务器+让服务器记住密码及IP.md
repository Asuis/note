1.安装与进入服务器

sudo apt-get update  #如有需要，会更新
sudo apt-get install openssh-server #安装ssh，中间选择y
sudo ps -e |grep ssh  #如果有sshd说明ssh服务已经启动，如果没有，输入sudo service sshd restart启动

    1
    2
    3

出现sshd

sudo gedit /etc/.ssh/sshd_config  #打开配置文件（里边有服务器端口号，port）

    1

#PermitRootLogin without-password #注释掉
PermitRootLogin yes #手动添加

    1
    2

保存。
现在输入，比如：

ssh work@192.168.0.222

    1

再输入密码就能登陆了。
上边work为用户名，@后边为主机IP。

2.SSH记住登录密码和ip地址
但是如果经常登录服务器，每次都要输入IP和密码，麻烦，我们可以通过设置，只需要服务器主机名自动登录。
（1）本地生成一对公钥密钥：

ssh-keygen -t rsa

    1

过程中会提示要输入生成的目录，提示信息括号里边有，复制就行，放到local用户的~/.ssh目录下。会生成：id_rsa（密钥） id_rsa.pub（公钥）
（2）然后就要把公钥放到服务器上：

scp ~/.ssh/id_rsa.pub work@192.168.0.222:～/.ssh/authorized_keys

    1

或者：

ssh-copy-id ~/.ssh/id_rsa.pub work@192.168.0.222

    1

ssh-copy-id这个命令会自动识别服务器～/.ssh/authorized_keys，将公钥复制进去。
（3）本地需要保存ssh登陆主机的相关信息，在用户根目录下的.ssh文件内创建config文件，保存ssh登陆主机的相关信息，这样就省得输入了：

gedit ~/.ssh/config

    1

打开配置文件后输入：

Host AAAAA #AAAAA为服务器主机名
HostName 192.168.0.222 #写服务器ip地址
User work #work为登陆用户名,不是自己电脑的名字
Port 22 #主机端口，默认是22
IdentityFile /home/me/.ssh/id_rsa.pub #自己生成的公钥的文件路径

    1
    2
    3
    4
    5

比如：
一开始需要密码登陆时：需要输入：
ssh work@ 192.168.0.222
那么这个work就是 User 后边的内容;在我们登录服务器后，是这样的：
服务器
那么work@后边的alg-03就是服务器主机名，也就是Host后边的内容，AAAAA。
（4）在服务器设置一下自动检验的信息：
打开/etc/ssh/sshd_config这个文件

sudo gedit /etc/.ssh/sshd_config

    1

去掉下面几行前面“#”注释

RSAAuthentication yes 
PubkeyAuthentication yes 
AuthorizedKeysFile .ssh/authorized_keys

    1
    2
    3

（5）进入登陆用户根目录下的.ssh/目录下，建立一个authorized_keys文件，把自己上传的公钥添加进去：

cat ~/id_rsa.pub >> ~/.ssh/authorized_keys

    1

（6）最后就可以这样登录了：

ssh alg-03

    1

3.从本地上传文件到服务器 从服务器下载文件到本地
在终端输入：

scp  本地文件路径 服务器帐号名@服务器的ip地址:想要保存的路径    #从本地到服务器 
scp  服务器帐号名@服务器的ip地址:文件路径  本地保存路径    #从服务器到本地
--------------------- 
作者：sunyao_123 
来源：CSDN 
原文：https://blog.csdn.net/sunyao_123/article/details/74783582 
版权声明：本文为博主原创文章，转载请附上博文链接！