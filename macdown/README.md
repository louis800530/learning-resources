# 常用的 Markdown 語法

* [語法 1: 標題](#header)
* [語法 2: 換行與段落](#new_line_and_paragrah)
* [語法 3: 程式碼](#code)
* [語法 4: 連結](#link)
* [語法 5: List](#list)
* [語法 6: 字體效果](#format)
* [語法 7: 表格](#table)
* [語法 8: 數學公式](#math)

# <a name="header"></a>語法 1: 標題
語法：

```
# 標題 1
## 標題 2
### 標題 3
#### 標題 4
```

效果：
# 標題 1
## 標題 2
### 標題 3
#### 標題 4

# <a name="new_line_and_paragrah"></a>語法 2: 換行與段落

一般按 **Enter** 後會換行，在 Markdown 的格式中，需在要這行的結尾多加***兩個***以上的***空白***才會換行。

比如：

這一行不會換行，
因為沒有空白。

這一行會換行，  
因為有空白。

段落則是多空一行即可。比如：

段落一：

段落二：

水平線效果，可用 `---`。

---


# <a name="code"></a>語法 3: 程式碼
* 單行:  使用一個 `` ` ``。比如：`` `int a = 1` ``  效果：`int a = 1`
* 區塊：使用三個 `` ` ``。在區塊前需要加一行空白，或者是一個區塊的結束。比如：

	~~~
	```
	int a = 1;
	int b = 2;
	```
	~~~
    
	效果：
    
	```
	int a = 1;
	int b = 2;
	```
	
	也可以在區塊上加程式語言的標註，有些編輯器會認程式的語法，為關鍵字加上顏色，方便閱讀，比如：
    
	~~~
	```scala
	val a = 10
	val one = { case 1 => "one" }
	```
	~~~
    
	效果：
    
	```scala
	val a = 10
	val one = { case 1 => "one" }
	```

ps: GitHub 不支援 `~~~` 區塊用法。    
    
# <a name="link"></a>語法 4: 連結

* email: 比如：`<service@17life.com>`, 效果：<service@17life.com>
* URL: 比如：`<http://www.17life.com>`, 效果：<http://www.17life.com>
* 超連結：`[text](url "title")`, title 可不填。比如：`[17Life 官網](http://www.17life.com "17Life，團購、宅配24小時出貨、優惠券、即買即用")` 效果：[17Life 官網](http://www.17life.com "17Life，團購、宅配24小時出貨、優惠券、即買即用")
* 圖片: `![text](url)`。比如：![17Life Logo](http://www.17life.com/Themes/PCweb/images/ppon-M2_LOGO.png)
* 參考：可以針對文章內所有的圖片與超連結做整理。比如：
`[17Life 官網][17Life_offical_site]` 或 `![17Life Logo][17Life_logo]`。  
再加上標註來源如：
`[17Life_offical_site]: http://www.17life.com "17Life，團購、宅配24小時出貨、優惠券、即買即用"` 或 
`[17Life_logo]: http://www.17life.com/Themes/PCweb/images/ppon-M2_LOGO.png`

效果：

[17Life 官網][17Life_offsite]  
![17Life Logo][17Life_logo]  

[17Life_offsite]: http://www.17life.com "17Life，團購、宅配24小時出貨、優惠券、即買即用"
[17Life_logo]: http://www.17life.com/Themes/PCweb/images/ppon-M2_LOGO.png "17Life Logo"

ps: 雖然圖片的連結，也可以放 title，比如 `![text](url "title")` 但是在 github 上顯示會有問題，就建議不要用。

* 錨點 (Atom 不支援)：與 HTML 相同。比如：在**語法 1** 加了 `# <a name="header"></a>語法 1: 標題`, 則 `[回語法 1](#header)` 則可直接跳到這個錨點。[回語法 1](#header)


# <a name="list"></a>語法 5: List

unsort list 使用 `*`開頭，sort list 使用**數字+`.`+空白**開頭。MacDown 使用 list 時，在前面要先空一行，或者是一個區塊的結果，否則會失敗，Atom 則不會。

需要有階層式的顯示時，用 tab 縮排。

比如：

```
我不空一行
* list
```

效果：
我不空一行
* list

語法：

```
* unsort list
    * A
        * a
        * b  
    * B
* sort list
    1. 123 
    2. 123
    3. 123 
* task list
    1. [x] I can render checkbox list syntax
    	* [x] I support nesting
    	* [x] I support ordered *and* unordered lists
    2. [ ] I don't support clicking checkboxes directly in the html window
```

效果：

* unsort list
    * A
        * a
        * b  
    * B
* sort list
    1. 123 
    2. 123
    3. 123 
* task list
    1. [x] I can render checkbox list syntax
    	* [x] I support nesting
    	* [x] I support ordered *and* unordered lists
    2. [ ] I don't support clicking checkboxes directly in the html window


# <a name="format"></a>語法 6: 字體效果
* 粗體：`**bold**`, 效果：**bold**
* 斜體：`*italic*`, 效果：*italic*
* 刪去：`~~strike through~~`，效果：~~strike through~~
* 底線：`_underline_`, 效果：_underline_ (Atom 不支援)
* Highlight: `==Highlight==`, ==Highlight== (Atom 不支援)
* Superscript: `hoge\^(fuga)`, hoge^(fuga) (Atom 不支援)

# <a name="table"></a>語法 7: 表格

```
This is a table:

First Header  | Second Header
------------- | -------------
Content Cell  | Content Cell
Content Cell  | Content Cell

You can align cell contents with syntax like this:

| Left Aligned  | Center Aligned  | Right Aligned |
|:------------- |:---------------:| -------------:|
| col 3 is      | some wordy text |         $1600 |
| col 2 is      | centered        |           $12 |
| zebra stripes | are neat        |            $1 |
```

This is a table:

First Header  | Second Header
------------- | -------------
Content Cell  | Content Cell
Content Cell  | Content Cell

You can align cell contents with syntax like this:

| Left Aligned  | Center Aligned  | Right Aligned |
|:------------- |:---------------:| -------------:|
| col 3 is      | some wordy text |         $16<br />00 |
|               |                 |               |
| col 2 is      | centered        |           $12 |
| zebra stripes | are neat        |            $1 |


inline format table

Option name         | Markup           | Result if enabled     |
--------------------|------------------|-----------------------|
Intra-word emphasis | So A\*maz\*ing   | So A<em>maz</em>ing   |
Strikethrough       | \~~Much wow\~~   | <del>Much wow</del>   |
Underline				| \_So doge\_      | <u>So doge</u>        |
Quote               | \"Such editor\"  | <q>Such editor</q>    |
Highlight           | \==So good\==    | <mark>So good</mark>  |
Superscript         | hoge\^(fuga)     | hoge<sup>fuga</sup>   |
Autolink            | http://t.co      | <http://t.co>         |

# <a name="math"></a>語法 8: 數學公式

數學公式的寫法，主要是依據 [LaTeX/Mathematics](https://en.wikibooks.org/wiki/LaTeX/Mathematics)。每種工具不一定會有支援，且語法會有些許的不同。

MacDown 語法：

```
\\[
    A^T_S = B
\\]
```

MacDown 效果：

\\[
    A^T_S = B
\\]

Atom 可改用 **markdown-preview-plus** ，需要顯示數學公式時，按 `ctrl+shift+x`。  
Atom 語法

```
\[
    A^T_S = B
\]
```
\[
    A^T_S = B
\]


# <a name="ref"></a>參考資料
* [MarkDown](http://daringfireball.net/projects/markdown/syntax)
* [MarkDown Wiki](https://en.wikipedia.org/wiki/Markdown)
* [MacDown](http://macdown.uranusjr.com/)
* [GitHub Atom](https://atom.io)
* [GitHub Flavored Markdown](https://guides.github.com/features/mastering-markdown/)
* [Mou](http://25.io/mou/)
* [LaTeX/Mathematics](https://en.wikibooks.org/wiki/LaTeX/Mathematics)
* [Markdown Blog](https://logdown.com/ "要錢")