Установить на сервере JRE, версия не ниже 11.0
Скопировать данную папку на рабочий диск на сервере.
Запустить из этой папки Calibration.bat (либо java -jar Calibration.jar)
Дождаться появления в командном окне сообщения "Started CalibrationApplication in ??.?? seconds"
В командном окне браузера набрать http://localhost:8080/
При работе через удаленный сервер вместо licalhost указать реальный IP-адрес сервера
В окне браузера нажать кнопку "Выберите файл" и выберите на локальном компьютере файл, содержащий калибровочную таблицу.
Файл должен быть в формате файла 36_6, приведенного для примера в этой папке и не иметь расширения
Нажмите кнопку "Соэдать аддон"
Появится сообщение "Аддон для емкости ?????? сформирован", где ?????? - имя файла с калибровочной таблицей
В ниэней части страницы появится ссылка для скачивания аддона. Щелкните ссылку, файл Calibration_??????.L5X окажется в папке автозагрузки.
Импортируйте полученный файл в качестве аддона в программу RSLogix5000, указав при импорте нужгое имя. По умолчанию будет имя Calibration_Header

Папку parts-dir не удалять!!!
В папке upload-dir будут сохраняться копии аддонов на сервере.