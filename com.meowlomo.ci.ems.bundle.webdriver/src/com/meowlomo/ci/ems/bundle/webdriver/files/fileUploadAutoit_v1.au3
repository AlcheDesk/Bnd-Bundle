$CmdLine[0] ; = 2 参数的总数量，不虚赋值
$CmdLine[1] ; = 上传文件路径
;在10秒内等待打开窗口出现
;WinWait("文件上传" Or "Open","",10)
WinWait("[CLASS:#32770]","",10)
;ControlFocus("title","text",ClassnameNN) 识别Window窗口
;ControlFocus("文件上传" Or "Open", "","")
ControlFocus("[CLASS:#32770]", "","")
;向“文件名”输入框内输入本地文件的路径
;ControlSetText("文件上传" Or "Open", "", "Edit1" Or "Edit", "test.eml")
ControlSetText("[CLASS:#32770]", "", "Edit1", $CmdLine[1])
Sleep(2000)
;单击打开按钮
ControlClick("[CLASS:#32770]", "","Button1");