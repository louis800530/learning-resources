#mac下創建捷徑

sudo ln -s spark-2.0.2-bin-hadoop2.7 spark

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

exit

scp .ssh/id_rsa.pub louis@node1:/home/louis/.ssh

cd .ssh

cat id_rsa.pub >> authorized_keys