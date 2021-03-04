<h1><img src="http://enroute.osgi.org/img/enroute-logo-64.png" witdh=40px style="float:left;margin: 0 1em 1em 0;width:40px">
OSGi enRoute Archetype</h1>

This repository represents a template workspace for bndtools, it is the easiest way to get started with OSGi enRoute. The workspace is useful in an IDE (bndtools or Intellij) and has support for [continuous integration][2] with [gradle][3]. If you want to get started with enRoute, then follow the steps in the [quick-start guide][1].

以上是BndTools生成的项目的原始自述<br>
以下是关于此项目的Meowlomo的说明<br>

1.2018.12以后，所有的VMC的bundle在此项目中生成，并保持更新和维护<br>
2.此WorkSpace是所谓的Bnd workspace项目,一般而言，需要安装BndTools插件，并保证与Eclipse版本匹配（并不总是推荐使用最新的Eclipse，以免BndTools无法使用的情形)。<br>
3.当需要添加新Bundle时，会有些麻烦，Repo的被动更新者们需要重新加载此bnd workspace（否则子项目不会添加进去）<br>
4.修改代码后BndTools会自动生成bundle(jar格式)在项目的Generated目录下,同时可以使用主目录下的copy脚本(copyBundles.bat)<br>
5.需要使用Bndtools Perspective打开Eclipse，左下角会有Repositories 的小窗体。展开Central,能够看到在cnf项目（一般而言是最上面一个）中的central.maven里定义的bundle. [maven repo][4]里面的包（满足OSGi规范定义manifest.mf的）可以定义在此（使用buildr规范)<br>
6.OSGi环境调试一般可以使用com.meowlomo.ci.ems.bundle.console项目的run.bndrun来运行（但不保证总是可用）<br>
7.每个bnd bundle项目需要编辑项目下的bnd.bnd来保证bndtools生成合适的满足OSGi规范的bundle(jar文件形式存在),这是作为使用者与学习者需要自行来掌握的部分,此处不再展开详述<br>

[1]: http://enroute.osgi.org/quick-start.html
[2]: http://enroute.osgi.org/tutorial_base/800-ci.html
[3]: https://www.gradle.org/
[4]: https://mvnrepository.com/
