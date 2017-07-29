# Tensorflow

## Contents
* [Quick Start](#1)
* [Build Graph](#2)
* [Exporting and Importing a Model](#3)
* [Save and Load Parameter](#4)
* [Supervisor](#5)
* [Distribute training](#6) 

## <a name="1"></a>Quick Start
###Sample
****
```python
import tensorflow as tf

x = tf.placeholder(tf.float32, [3])
y = tf.placeholder(tf.float32, [1])
w = tf.Variable(tf.truncated_normal([3, 1]))
b = tf.Variable(tf.constant(0.1, shape=[1]))
prediction = tf.matmul(x, w) + b
loss = y - prediction
optimizer = tf.train.AdamOptimizer()
train = optimizer.minimize(loss)

init = tf.global_variables_initializer()
sess = tf.Session()
sess.run(init)
_, result = sess.run([train, prediction],
					 feed_dict={x: [0.45, 0.88, 0.23], y: [0]})
```
上述程式碼中x跟y分別為input features跟labels，tf.placeholder代表在tensorflow中是待輸入的容器，直到真正輸入之前都不會有值，並且只要有tf.placeholder存在，運作模型時必定要有輸入．

w與b代表神經層的權重及偏壓，使用tf.Variable的參數即為每次迭代中必須被更新的數值，tf.truncated_normal定義了變數的初始值為截斷常態分佈，其中均值為0，標準差為1，tf.constant則是定義初始值為常數，在這裡是0.1．

將x與w相乘加b得到prediction，loss則是代表lebels的y與prediction的差．

完成基本架構後我們呼叫tf.train.AdamOptimizer()來當我們的最佳化方法，並且針對我們自定義的loss找極小值．

tf.global\_variables\_initializer()是初始化所有變數的方法，只有經過初始化的變數才能使用，tf.Session()預設開啟本地端的訓練伺服器，呼叫sess.run()才是真正把我們剛剛所建構的模型實體化，在使用sess.run()之前我們所寫的東西相當於建築物的藍圖而已，並不實際存在．

以sess.run([], feed\_dict={})這行來看，[]中的物件是我們實際想讓tensorflow運作的東西，也是實際上我們會得到的值，而參數feed\_dict則是我們模型接受輸入的地方．

## <a name="2"></a>Build Graph
###RNN with GRU

經過上一段簡單的示範後，我們將進入更實際的應用，實作一段真正的RNN模型
****
```python
def build_graph(num_hidden,
                time_step,
                output_size,
                num_layers,
                keep_prob_tensor,
                penalty_coefficient):
    global_step = tf.Variable(0, name="global_step", trainable=False)
    data = tf.placeholder(tf.float32, [None, time_step, num_hidden])
    target = tf.placeholder(tf.float32, [None, time_step, num_hidden])
    batch_size_tensor = tf.placeholder_with_default(
        tf.shape(data)[0], shape=[]
    )
    cell = tf.contrib.rnn.GRUCell(num_hidden)
    cell = tf.contrib.rnn.DropoutWrapper(
        cell,
        output_keep_prob=keep_prob_tensor
    )
    cell = tf.contrib.rnn.MultiRNNCell([cell] * num_layers)
    init_state = cell.zero_state(batch_size_tensor, tf.float32)
    val, state = tf.nn.dynamic_rnn(cell,
                                   data,
                                   dtype=tf.float32,
                                   sequence_length=length(data),
                                   initial_state=init_state)

    weight = tf.Variable(tf.truncated_normal([num_hidden, output_size]))
    bias = tf.Variable(tf.constant(0.1, shape=[output_size]))
    val = tf.reshape(val, [-1, num_hidden])
    val = tf.nn.dropout(val, keep_prob_tensor)

    prediction = tf.matmul(val, weight) + bias
    predic_norm = tf.nn.l2_normalize(prediction[:, :output_size - 3], dim=1)
    prediction = tf.concat([predic_norm, prediction[:, -3:]], 1)

    target_out = tf.reshape(target[:, :, 400:], [-1, output_size])
    target_norm = tf.nn.l2_normalize(target_out[:, :output_size - 3], dim=1)
    target_out = tf.concat([target_norm, target_out[:, -3:]], 1)

    mask = tf.sign(tf.reduce_max(tf.abs(target_out), 1, keep_dims=True))
    predict_mask = prediction * mask
    vec_loss = tf.losses.cosine_distance(
        target_out[:, :output_size - 3],
        predict_mask[:, :output_size - 3],
        dim=1
    )
    valid_input_num = tf.count_nonzero(mask[:, 0], dtype=tf.int32)
    total_input_num = tf.shape(mask, out_type=tf.int32)[0]
    ratio = valid_input_num / total_input_num
    vec_loss = vec_loss / tf.cast(ratio, tf.float32)
    rare_class_penalty = tf.add(tf.scalar_mul(penalty_coefficient,
                                              target_out[:, -2]), 1)
    p_error = tf.squared_difference(target_out[:, -2], predict_mask[:, -2])
    n_error = tf.squared_difference(target_out[:, -1], predict_mask[:, -1])
    purchase_loss = tf.reduce_mean(tf.multiply(rare_class_penalty, p_error))
    unpurchase_loss = tf.reduce_mean(tf.multiply(rare_class_penalty, n_error))
    consuming_loss = purchase_loss + unpurchase_loss
    loss = vec_loss + 0.5 * consuming_loss

    tf.summary.scalar("loss", loss)
    optimizer = tf.train.AdamOptimizer()
    train = optimizer.minimize(loss, global_step=global_step)

    model = {"global_step": global_step,
             "data": data,
             "target": target,
             "target_out": target_out,
             "prediction": prediction,
             "loss": loss,
             "train": train}
    return model
```

在這段程式碼中我們需要六個參數幫助我們建構完整的RNN架構，分別如下：

* num_hidden 隱藏神經元的數目
* time_step 考慮的時序
* output_size 輸出層的大小
* num_layers 隱藏神經層的層數
* keep\_prob\_tensor 訓練中隨機捨棄的神經元比例，1為完整保留，0為徹底捨去
* penalty_coefficient 針對樣本不平衡的懲罰因子

```python
global_step = tf.Variable(0, name="global_step", trainable=False)
```
global_step紀錄了總共訓練多少次，在重新訓練和分散式訓練中非常重要，name是匯出模型時給定變數的名稱，trainable必定要關閉，才不會在訓練中被更新變數的步驟使用梯度下降更新了，所有變數都預設為True

```python
data = tf.placeholder(tf.float32, [None, time_step, num_hidden])
target = tf.placeholder(tf.float32, [None, time_step, num_hidden])
batch_size_tensor = tf.placeholder_with_default(tf.shape(data)[0], shape=[])
```
data與target分別是features和labels，tf.float32是變數類型，整套模型都建議要一致且明示，否則容易出現不可預期的錯誤，[None, time\_step, num\_hidden]由左至右是批次大小，時序，隱神經元數，批次大小可由輸入當下再決定所以可設為None，時序標明模型考慮了多少時刻之前的輸入．

batch_size _tensor是非常重要的細節，一般來說訓練時都會使用批次訓練，預測時則使用單一輸入，但tensorflow在匯出模型時如果是常數會變成模型的一部分不可更動，導致我們無法更改批次大小，所以我們必須把批次大小設計成容器類型並賦予預設值，也就是輸入的第一個維度，這樣一來我們可以匯出同一套模型在訓練與產品環境之間無痛轉換，依照輸入變動批次大小．

```python
cell = tf.contrib.rnn.GRUCell(num_hidden)
cell = tf.contrib.rnn.DropoutWrapper(cell,output_keep_prob=keep_prob_tensor)
cell = tf.contrib.rnn.MultiRNNCell([cell] * num_layers)
init_state = cell.zero_state(batch_size_tensor, tf.float32)
val, state = tf.nn.dynamic_rnn(cell,
                               data,
                               dtype=tf.float32,
                               sequence_length=length(data),
                               initial_state=init_state)
```
本段為RNN的本體，GRUCell為我們構築出激活函數的block，只要給定需要的數目就能完成，DropoutWrapper幫助我們在訓練時捨棄一定數量的神經元可減少overfitting，MultiRNNCell使我們可以把神經層疊高到我們想要的層數，cell.zero_ state可以初始化神經層並賦值，最後使用dynamic_rnn串接所有元素並且在訓練時可依照每個批次的時間長度不同而改變掃描的大小，val是類神經網路的輸出，state是隱藏神經元最後的狀態．

```python
weight = tf.Variable(tf.truncated_normal([num_hidden, output_size]))
bias = tf.Variable(tf.constant(0.1, shape=[output_size]))
val = tf.reshape(val, [-1, num_hidden])
val = tf.nn.dropout(val, keep_prob_tensor)

prediction = tf.matmul(val, weight) + bias
predic_norm = tf.nn.l2_normalize(prediction[:, :output_size - 3], dim=1)
prediction = tf.concat([predic_norm, prediction[:, -3:]], 1)

target_out = tf.reshape(target[:, :, 400:], [-1, output_size])
target_norm = tf.nn.l2_normalize(target_out[:, :output_size - 3], dim=1)
target_out = tf.concat([target_norm, target_out[:, -3:]], 1)
```
這段程式碼實作了權重及偏壓的初始化，添加dropout層，計算預測值，針對預測屬性的部分做正規化後再接回預測購買的值，針對labels處理掉我們不需要的維度(使用者向量)，一樣對labels商品屬性部分正規化，最後接回是否購買的標籤．

```python
mask = tf.sign(tf.reduce_max(tf.abs(target_out), 1, keep_dims=True))
predict_mask = prediction * mask
vec_loss = tf.losses.cosine_distance(
    target_out[:, :output_size - 3],
    predict_mask[:, :output_size - 3],
    dim=1
)
```
因為我們的輸入有針對time step不足的example補零，這樣一來造成就算實際上labels出現全為零，prediction有著bias的存在就算輸入為零，輸出也不等於零，一旦我們直接計算prediction與labels的誤差，會把這些不該算的項目也算進去，所以我們做了個遮罩，對於labels全為零的example理所當然也不該計算prediction，把不該計算的項目濾掉後就可以計算商品屬性的cosine distance error．

```python
valid_input_num = tf.count_nonzero(mask[:, 0], dtype=tf.int32)
total_input_num = tf.shape(mask, out_type=tf.int32)[0]
ratio = valid_input_num / total_input_num
vec_loss = vec_loss / tf.cast(ratio, tf.float32)
```
當然，我們做了濾波勢必就要考慮到取loss的平均時，分母也有了變化，可以從遮罩為1的地方計算有效數目，反推出真正需要被分配loss的商品數目．

```python
rare_class_penalty = tf.add(tf.scalar_mul(penalty_coefficient,
                                              target_out[:, -2]), 1)
p_error = tf.squared_difference(target_out[:, -2], predict_mask[:, -2])
n_error = tf.squared_difference(target_out[:, -1], predict_mask[:, -1])
purchase_loss = tf.reduce_mean(tf.multiply(rare_class_penalty, p_error))
unpurchase_loss = tf.reduce_mean(tf.multiply(rare_class_penalty, n_error))
consuming_loss = purchase_loss + unpurchase_loss
```
這一段考慮了正負樣本不平衡的資料必須使用懲罰因子來平衡loss，分別計算正負的誤差，乘以個別懲罰係數，相加就是總共的是否購買loss．

```python
loss = vec_loss + 0.5 * consuming_loss
tf.summary.scalar("loss", loss)
optimizer = tf.train.AdamOptimizer()
train = optimizer.minimize(loss, global_step=global_step)

model = {"global_step": global_step,
         "data": data,
         "target": target,
         "target_out": target_out,
         "prediction": prediction,
         "loss": loss,
         "train": train}
return model
```
最後把兩種loss相加可以得到總loss，tf.summary.scalar("loss", loss)是為了之後可以顯示在tensorboard上做訓練參數視覺化而做的匯出準備，一樣選擇最佳化方法，針對我們的loss取最小極值，記得放入global_step使得每次訓練可以自動記錄訓練了幾次，最後輸出我們的model．

## <a name="3"></a>Exporting and Importing a Model

Tensorflow 的匯出邏輯要分成兩個部分，第一是模型，第二是變數，只有當需要把製作完成的模型給其他環境使用時才需要匯出模型，一般來說單純地重複訓練只需要匯出訓練完的變數即可，以下針對匯出模型做說明．

###Exporting
****
```python
tf.add_to_collection("data", data)
tf.add_to_collection("prediction", prediction)
inputs = {"x": utils.build_tensor_info(data)}
outputs = {"outputs": utils.build_tensor_info(prediction)}
signature = signature_def_utils.build_signature_def(
    inputs=inputs,
    outputs=outputs,
    method_name=signature_constants.PREDICT_METHOD_NAME
)
version = 1
builder = saved_model_builder.SavedModelBuilder(
    "data/RNN/%d" % version
)
init = tf.global_variables_initializer()
with tf.Session(server.target) as sess:
    sess.run(init)
    builder.add_meta_graph_and_variables(
        sess,
        tags=[tag_constants.SERVING],
        clear_devices=True,
        signature_def_map={"recommendation": signature}
    )
    builder.save()
```
tf.add_to_collection()可以把tensorflow 的數據結構加入自己命名的集合用於打包匯出，inputs = {"x": utils.build\_tensor\_info(data)}目的在於擷取數據的資訊以利Tensorflow serving，如果不使用Tensorflow serving則不需要做這一步，同樣signature\_def\_utils.build\_signature\_def()打包了所有Tensorflow serving需要的資訊，參數method\_name可以看官方文件有幾種固定的定義， 依照自己的使用場景選擇．從saved\_model\_builder.SavedModelBuilder()開始是所有匯出模型的通常步驟，呼叫saved\_model\_builder.SavedModelBuilder()的當下會馬上幫你建立自己指定的版號資料夾，設定為不能覆蓋，所以匯出之前要確定無重複名稱資料夾．在匯出模型之前別忘記Tensorflow的所有結構都必須先初始化所以要啟動tf.Session()，其中server.target是分散式訓練中指定該訓練機儲存，如果不指定會出現錯誤，只有在真正單機訓練時可以不指定，預設就是本地端存檔，sess.run(init)初始化，builder.add\_meta\_graph\_and\_variables()這一步會在指定版號的資料夾中建立variables資料夾，內有變數容器，而最後的builder.save()則是在版號資料夾下建立saved_model.pb，是完整的模型構造．

以上步驟所建立的單純只有模型，訓練過後的參數在這一步都尚未建立．

###Importing
****
```python
sess = tf.Session()
saver = tf.train.import_meta_graph('data/RNN/m.meta')
saver.restore(sess, "data/RNN/m")

data = tf.get_collection("data")[0]
prediction = tf.get_collection("prediction")[0]
```
上面三行是匯入模型的標準流程，匯入後必須把模型的內容使用tf.get_collection()賦予到我們自訂的變數之中，就可以重複利用了，注意，同樣的命名空間可以放不只一個變數所以會有[]出現．
## <a name="4"></a>Save and Load Parameter

一般來說儲存訓練過的參數會比匯出模型要簡單得多，以下是存檔跟讀檔範例

###Save
****
```python
saver = tf.train.Saver()
sess = tf.Session()
sess.run([train], feed_dict={})
saver.save(sess, 'data/RNN/m')
```
使用tf.train.Saver()呼叫存檔器，並且開始訓練自己的參數，最後使用saver.save(sess, 'data/RNN/m')把訓練過的參數存在指定資料夾，這邊要注意檔案是存在RNN之下，名稱為m.index, m.meta, m.data-00000-of-00001 以及 checkpoint
###Load
****
```python
saver = tf.train.Saver()
sess = tf.Session()
saver.restore(sess, "data/RNN/m")
```
如果要讀取參數重複訓練可以使用saver.restore()來回復變數最後記錄下來的值．

## <a name="5"></a>Supervisor

在分散式訓練中如果都用單機的方法實作有許多細節要注意，所以我們將會使用Supervisor這強大的工具包來幫我們達到一些目的．

Supervisor可以幫我們達到初始化變數，每隔一段訓練時間存檔，監控訓練步數，紀錄我們想展示在Tensorboard的各種參數，除了匯出完整模型以外的功能幾乎都包辦了
###Sample
****
```python
tf.summary.scalar('loss', loss)

...

summary_op = tf.summary.merge_all()
saver = tf.train.Saver(sharded=True)
...

sv = tf.train.Supervisor(is_chief=(args.task == 0),
                         logdir="./data/RNN",
                         init_op=init,
                         summary_op=None,
                         saver=saver,
                         global_step=global_step,
                         save_model_secs=600)
with sv.prepare_or_wait_for_session(server.target) as sess:
	sess.run([train], feed_dict={})
	step = tf.train.global_step(sess, global_step)
	while not sv.should_stop() and step < 1000:
		if step % 100 == 0:
			_, summ = sess.run([train, summary_op], feed_dict={})
			sv.summary_computed(sess, summ)
		else:
			sess.run([train], feed_dict={})
```
tf.summary.scalar()可以紀錄每一步的任何參數變化，名稱為Tensorboard上顯示的名稱，tf.summary.merge\_all()把所有想紀錄的參數打包在一起，Supervisor裡的summary\_op必定要設為None，因為直到TF1.1都還是有無解BUG，is\_chief是指定某個worker為master，負責儲存checkpoint的主要機器，init_op需要設置初始化方法，一般都是放tf.global\_variables\_initializer()，logdir是checkpiont檔的存放位置也是讀取前一次訓練參數的目錄，saver的初始化設定一定要是sharded=True，否則在分散式訓練的環境下要手動複製checkpoint及相關檔案到所有訓練的主機目錄之下，有打開的情況下存檔只需要放在master的worker機器上即可，最後save\_model\_secs所設置的秒數代表訓練中間隔多久會自動存檔一次．

如上一段所說，Supervisor裡的summary\_op損毀，所以這裡使用sv.summary\_computed(sess, summ)每訓練100步就紀錄一次用於tensorboard的訓練參數．

## <a name="6"></a>Distribute training

###Sample
****
```python
def main():
	parameter_servers = ["192.168.1.53:2223"]
	workers = ["192.168.1.53:2222",
	           "192.168.1.54:2222",
	           "192.168.1.73:2222",
	           "192.168.1.74:2222"]
	
	cluster = tf.train.ClusterSpec({"ps": parameter_servers,
	                                    "worker": workers})
	server = tf.train.Server(cluster,
	                         job_name=args.job,
	                         task_index=args.task)
	if args.job == "ps":
	    server.join()
	elif args.job == "worker":
	    work(server, cluster)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--job",
        type=str,
        default="",
        help="Either 'ps' or 'worker'"
    )
    parser.add_argument(
        "--task",
        type=int,
        default=0,
        help="Index of task within the job"
    )
    args = parser.parse_args()
    main()
```

Tensorflow的分散式訓練群集需要一開始就指定每台主機IP及工作角色，ps用於參數交換，設置一台即可，消耗資源不多，worker是主要訓練的機器以task_index來區分，ps一啟動就不需要任何程式碼，直到確認所有工作完成手動關掉即可，work()則是我們需要為每台機器撰寫的分散式訓練的方法．

```
python3 distribute_rnn_train.py --job ps --task 0
python3 distribute_rnn_train.py --job worker --task 0
python3 distribute_rnn_train.py --job worker --task 1
python3 distribute_rnn_train.py --job worker --task 2
python3 distribute_rnn_train.py --job worker --task 3
```
上面指令的前兩行輸入在同一主機，ps只負責交換訓練參數，訓練完以後要手動kill，worker 0代表master，通常是模型跟訓練後參數存放的位置．

剩餘三行指令分別輸入在不同台主機來分配訓練工作．