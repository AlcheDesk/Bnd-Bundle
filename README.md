# What is this Repo?

This repository is the home for the sources of some useful Bndtools Project Templates. These are provided as bundles which you can include in your workspace to add new project templates.

# How do I use these?

You can add the template bundles to any repository in your workspace, but if you use the [Bndtools Workspace Template](https://github.com/bndtools/workspace) then they're included by default.

以上是BndTools生成的项目的原始自述<br>
以下是关于此项目的Meowlomo的说明<br>

1.2018.12以后，所有的VMC的bundle在此项目中生成，并保持更新和维护<br>
2.此WorkSpace是所谓的Bnd workspace项目,一般而言，需要安装BndTools插件，并保证与Eclipse版本匹配（并不总是推荐使用最新的Eclipse，以免BndTools无法使用的情形)。<br>
3.当需要添加新Bundle时，会有些麻烦，Repo的被动更新者们需要重新加载此bnd workspace（否则子项目不会添加进去）<br>
4.修改代码后BndTools会自动生成bundle(jar格式)在项目的Generated目录下,同时可以使用主目录下的copy脚本(copyBundles.bat)<br>
5.需要使用Bndtools Perspective打开Eclipse，左下角会有Repositories 的小窗体。展开Central,能够看到在cnf项目（一般而言是最上面一个）中的central.maven里定义的bundle. [maven repo][4]里面的包（满足OSGi规范定义manifest.mf的）可以定义在此（使用buildr规范)<br>
6.OSGi环境调试一般可以使用com.meowlomo.ci.ems.bundle.console项目的run.bndrun来运行（但不保证总是可用）<br>
7.每个bnd bundle项目需要编辑项目下的bnd.bnd来保证bndtools生成合适的满足OSGi规范的bundle(jar文件形式存在),这是作为使用者与学习者需要自行来掌握的部分,此处不再展开详述<br>

[1]: https://enroute.osgi.org/tutorial/020-tutorial_qs.html
[2]: http://enroute.osgi.org/tutorial_base/800-ci.html
[3]: https://www.gradle.org/
[4]: https://mvnrepository.com/
