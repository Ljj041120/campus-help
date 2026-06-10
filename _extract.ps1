$word = New-Object -ComObject Word.Application
$word.Visible = $false
$doc = $word.Documents.Open("C:\Users\ASUS\Desktop\综合实训1\《软件工程综合实训1》实验指导书20260507.doc")
$text = $doc.Content.Text
$doc.Close()
$word.Quit()
Write-Host $text
