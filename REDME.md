## 运行环境
jdk8或更高版本
## 指令格式
>java -jar [jar包路径]  
>     -i [输入路径] 可选，默认为当前目录下的avatar文件夹  
>     -o [输出路径] 可选，默认为当前目录下的out文件夹  

>例如:> java -jar D:\code\sprite-util\target\sprite-util-1.1.jar -i E:\Resources\ArknightsGameResource\avatar -o E:\Resources\out
> 
读取“E:\Resources\ArknightsGameResource\avatar”路径下的图片,输出精灵图和css到“E:\Resources”路径下

## 使用方法
运行程序需要先将要制作为精灵图的干员图片保存到本地，并放在指定文件夹下
![img_1.png](img_1.png)
程序运行结束后，需要对sprite_avatar.css文件中的url进行替换，才能正常使用
