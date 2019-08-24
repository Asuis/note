正在研究机器学习的全栈码农Dendrick Tan在博客上发布了一份教程+代码：用PyTorch实现将色块拼凑成的图片，转换为一幅Bob Ross风格的画作。

说到Bob Ross，这头蓬松的秀发你可能有点印象……

他在电视节目《欢乐画室》上教了11年画画，还推出了同名的美术用品品牌，也出版了不少教材。

他的画，基本上是这个风格：

量子位今天要介绍的这个教程，就叫drawlikebobross。

王新民 编译整理
量子位 出品 | 公众号 QbitAI
是什么？

drawlikebobross项目的目标是，将一张色块拼凑而成的图片转变成（看起来有点像）Bob Ross油画风格的图像，如下图所示：
怎么做？

获取数据

在我们开始训练网络之前，首先需要获取数据来构建数据集。幸运的是，通过谷歌搜索，我在Bob Ross Database - List of all Bob Ross paintings网站上找到了一个关于Bob Ross作品的数据集。

这个网站的优点是它包含所有的Bob Ross作品，并按照如下格式列出：

    http://www.twoinchbrush.com/images/painting1.png
    http://www.twoinchbrush.com/images/painting2.png
    http://www.twoinchbrush.com/images/painting3.png
    ……http://www.twoinchbrush.com/images/paintingN.png

开源代码中的scrapper.sh就是用来完成这项工作的。

数据预处理

由于我们的目标是将色块图片转换为Bob Ross风格的图像，所以我决定使用平均偏移滤波（mean shift filtering）来实现图像平滑操作，将得到的色块图像作为输入，原始图像作为输出。

为了最大限度地减少训练时间，我将大部分原始图像预处理成平滑的色块图片，并存储为HDF5格式。由于HDF5的快速可读写性能，我们能够快速地测试不同的神经网络结构，无需在训练时间内多次重复预处理数据，这样，就节省了大量的时间。

神经网络结构

这个项目所使用的网络结构叫做对抗自动编码器（Adversarial Autoencoder），也被简称为AAE。关于AAE，有一篇博客介绍（Adversarial Autoencoders）和一篇论文（[1511.05644] Adversarial Autoencoders）。

博客介绍中对AAE的评价是：“我觉得本文中最有趣的想法是不利用变分推理方法，而是使用对抗训练的方法将编码器的输出分布q（z | x）映射到任意先验分布p（z）的概念。”

△ 对抗自动编码器AAE的网络结构

将数据输入我们的模型

我们希望将色块图片输入到网络，输出为Bob Ross风格的图像。具体实现流程如下。

△ 利用AAE网络实现风格迁移

我选择使用PyTorch来实现这个模型，是因为我一直在用它，也是因为与TensorFlow相比，这个框架的API很好用，且保持着很强的一致性，自从用了它，效率提升了几十倍。

该模型的训练过程可以分为四个部分：

    models.py：神经网络的结构；
    loader.py：数据载入操作；
    trainer.py：训练网络的超参数配置；
    train.py：训练的启动文件；


通过这种方式，如果想改变神经网络的结构，只需要再次编辑models.py和trainer.py，很容易进行修改。

训练

网络训练的时间越长，则网络输出Bob Ross风格的图像效果越好。

由于我使用的是ThinkPad t460s，自带的GPU计算性能不好，所以我在AWS上租了一个g2工作站，花了大约一天的时间，运行了2500步，来训练这个模型。

另外

drawlikebobross的开源代码中还包含一个网页App，欢迎下载测试，告诉我们结果如何。

也强烈推荐想尝试PyTorch的同学，花两天时间用这样一个小项目练手~

我们整理了文章中提到的所有相关链接，在量子位公众号（QbitAI）对话界面回复“BobRoss”查看。

    今天AI界还有哪些事值得关注？在量子位（QbitAI）公众号会话界面回复“今天”，看我们全网搜罗的AI行业和研究动态。笔芯❤~

发布于 2017-04-09