## 运行环境
jdk8或更高版本
## 指令格式
>java -jar [jar包路径]  
>     -i，--input  [输入路径] 可选，默认为当前目录下的avatar文件夹  
>     -o，--output  [输出路径] 可选，默认为当前目录下的out文件夹  
>     -a，--all-images [是否生成所有图片的精灵图] 可选，默认为false，如果为true则生成所有图片的精灵图，否则只生成明日方舟指定干员图片的精灵图
>     -w, --max-width=[64-16384] 可选，精灵图最大宽度（默认：4096）
>     -h, --help 展示帮助信息 可选
>例如:> java -jar D:\code\sprite-util\target\sprite-util-1.3.jar -i E:\Resources\ArknightsGameResource\avatar -o E:\Resources\out -a
> 
读取“E:\Resources\ArknightsGameResource\avatar”路径下的所有图片,输出精灵图和css到“E:\Resources”路径下

## 使用方法
运行程序需要先将要制作为精灵图的干员图片保存到本地，并放在指定文件夹下
![img.png](image%2Fimg.png)
程序运行结束后，需要对sprite_avatar.css文件中的url进行替换，才能正常使用
