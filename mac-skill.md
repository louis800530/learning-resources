#mac下創建捷徑

sudo ln -s spark-2.0.2-bin-hadoop2.7 spark

#取消捷徑

sudo unlink xxx

#mac & zsh 環境變數設定

vi ~/.zshrc

export XXX_HOME="/usr/local/XXX"

export PATH=".:$XXX_HOME/bin"

source ~/.zshrc


#改host

sudo vi /etc/hosts 

#綁SSH KEY的方法

cd .ssh

ls -la

id_rsa.pub

cd ~

ssh louis@node1

mkdir .ssh

chmod 700 .ssh

exit

scp .ssh/id_rsa.pub louis@node1:/home/louis/.ssh

cd .ssh

cat id_rsa.pub >> authorized_keys

chmod 600 authorized_keys

#修改檔案擁有者

sudo chown -R louis.louis spark

#改變隱藏資料夾屬性

chflags nohidden /Users/louis/Library

#列出使用中的程式且抓取關鍵字

ps -ef | grep spark