package org;

import api.ApiClient;
import api.FootballApi;
import api.FrankfurterApi;
import api.WeatherApi;
import exceptions.ApiException;
import exceptions.FileProcessingException;
import format.CsvFormatter;
import format.Formatter;
import format.JsonFormatter;
import model.SaveMode;
import service.DataService;
import service.PollingService;
import service.ThreadService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class ConsoleUI {
    private final Scanner scanner = new Scanner(System.in);
    private final DataService service;
    private PollingService pollingService;

    public ConsoleUI(DataService service) {
        this.service = service;
    }

    public void runInteractiveMode() {
        while (true) {
            System.out.println();
            System.out.println("Выберите действие:");
            System.out.println("1. Получить и записать данные Api");
            System.out.println("2. Запустить параллельный опрос");
            System.out.println("3. Вывести содержимое файла");
            System.out.println("4. Выход");

            int choice = readInt();
            switch (choice) {
                case 1:
                    ApiClient api = choiceApi();
                    if (api == null) {
                        break;
                    }
                    processApi(api);
                    break;
                case 2:
                    parallelPoll();
                    break;
                case 3:
                    showFile();
                    break;
                case 4:
                    return;
                default:
                    System.out.println("Неверный пункт меню");
            }
        }
    }

    public void runAutoMode(String[] args) {
        if (args.length < 3) {
            System.out.println("Неверное количество параметров!");
            System.out.println("Api: weather, frankfurter, football");
            System.out.println("Использование:");
            System.out.println("weather result.json new");
            return;
        }
        String fileName = args[args.length - 2];
        String modeName = args[args.length - 1];
        Path path = Paths.get(fileName);
        Formatter formatter = getFormatter(path);
        SaveMode mode;
        switch (modeName.toLowerCase()) {
            case "new":
                mode = SaveMode.CREATE_NEW;
                break;
            case "append":
                mode = SaveMode.APPEND;
                break;
            default:
                System.out.println("Неизвестный режим: " + modeName);
                return;
        }
        for (int i = 0; i < args.length - 2; i++) {
            try {
                ApiClient api = parseApi(args[i]);
                service.save(api, formatter, path, mode);
                System.out.println(api.getName() + " сохранён");
                mode = SaveMode.APPEND;
            } catch (ApiException e) {
                System.out.println("Ошибка API: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Неизвестный API");
            } catch (FileProcessingException e) {
                System.out.println("Ошибка файла: " + e.getMessage());
            }
        }
    }

    private ApiClient parseApi(String apiName) {
        switch (apiName.toLowerCase()) {
            case "weather":
                return new WeatherApi();
            case "frankfurter":
                return new FrankfurterApi();
            case "football":
                return new FootballApi();
            default:
                throw new ApiException("Неизвестный API: " + apiName);
        }
    }

    private void processApi(ApiClient api) {
        Path path = choiceFile();
        if (path == null) {
            return;
        }
        SaveMode mode = determineSaveMode(path);
        if (mode == null) {
            return;
        }
        try {
            Formatter format = getFormatter(path);
            service.save(api, format, path, mode);
            System.out.println("Успешно сохранено.");
        }
        catch (FileProcessingException e) {
            System.out.println("Ошибка c форматированием: " + e.getMessage());
        }
        catch(ApiException e) {
            System.out.println("Ошибка Api: " + e.getMessage());
        }
    }

    private ApiClient choiceApi() {
        int choice = 0;
        while(true) {
            System.out.println("Выберите Api:");
            System.out.println("1. WeatherApi(Узнать погоду)");
            System.out.println("2. FrankfurterApi(Курс валют)");
            System.out.println("3. FootballApi(Результаты матчей)");
            System.out.println("4. Назад.");
            choice = readInt();
            switch (choice) {
                case 1:
                    return new WeatherApi();
                case 2:
                    return new FrankfurterApi();
                case 3:
                    return new FootballApi();
                case 4:
                    return null;
                default:
                    System.out.println("Неверный выбор");
            }
        }
    }

    private Path choiceFile() {
        System.out.println("Введите имя файла:");
        String fileName = scanner.nextLine();
        if (fileName.isBlank()) {
            return null;
        }
        return Paths.get(fileName);
    }

    private void showFile() {
        Path path = choiceFile();
        if (path == null) {
            return;
        }
        Formatter formatter = getFormatter(path);
        while (true) {
            try {
                System.out.println();
                System.out.println("Что вывести?");
                System.out.println("1. Всё содержимое");
                System.out.println("2. Только данные конкретного API");
                System.out.println("3. Назад");
                int choice = readInt();
                switch (choice) {
                    case 1:
                        formatter.print(path);
                        return;
                    case 2:
                        ApiClient api = choiceApi();
                        if (api == null) {
                            continue;
                        }
                        formatter.printBySource(
                                path,
                                api.getName()
                        );
                        return;
                    case 3:
                        return;
                    default:
                        System.out.println("Неверный выбор");
                }
            } catch (FileProcessingException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private Formatter getFormatter(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".json")) {
            return new JsonFormatter();
        }
        if (fileName.endsWith(".csv")) {
            return new CsvFormatter();
        }
        throw new FileProcessingException(
                "Поддерживаются только .json и .csv"
        );
    }

    private int readInt() {
        while (true) {
            try {
                int value = scanner.nextInt();
                scanner.nextLine();
                return value;
            } catch (InputMismatchException e) {
                System.out.println("Введите число!");
                scanner.nextLine();
            }
        }
    }

    private void parallelPoll() {
        System.out.println("Введите максимальное количество потоков:");
        int maxThreads = readInt();
        if (maxThreads <= 0) {
            System.out.println("Количество потоков должно быть больше нуля.");
            return;
        }

        System.out.println("Введите интервал между запросами в секундах:");
        int intervalSeconds = readInt();

        if (intervalSeconds < 0) {
            System.out.println("Интервал не может быть отрицательным.");
            return;
        }

        List<ApiClient> apis = List.of(
                new WeatherApi(),
                new FrankfurterApi(),
                new FootballApi()
        );

        Path path = choiceFile();
        SaveMode mode = determineSaveMode(path);
        Formatter formatter;
        try {
            formatter = getFormatter(path);
        } catch (FileProcessingException e) {
            System.out.println("Ошибка формата файла: " + e.getMessage());
            return;
        }
        ThreadService threadService =
                new ThreadService(maxThreads);
        pollingService =
                new PollingService(threadService, service);

        try {
            pollingService.startPolling(
                    apis,
                    formatter,
                    path,
                    mode,
                    intervalSeconds
            );

            System.out.println();
            System.out.println("Периодический опрос запущен.");
            System.out.println("Введите stop для остановки.");

            while (true) {
                String command = scanner.nextLine();

                if (command.equalsIgnoreCase("stop")) {
                    pollingService.shutdown();
                    System.out.println("Периодический опрос остановлен.");
                    break;
                }

                System.out.println("Неизвестная команда. Для остановки введите stop.");
            }

        } catch (FileProcessingException e) {
            System.out.println("Ошибка при запуске опроса: " + e.getMessage());
            pollingService.shutdown();
        }
    }

    private SaveMode determineSaveMode(Path path) {
        if (!java.nio.file.Files.exists(path)) {
            return SaveMode.CREATE_NEW;
        }
        while (true) {
            System.out.println("Файл " + path + " уже существует, дозаписать в него? (да/нет)");
            String answer = scanner.nextLine().trim().toLowerCase();
            switch (answer) {
                case "да":
                case "д":
                case "yes":
                case "y":
                    return SaveMode.APPEND;
                case "нет":
                case "н":
                case "no":
                case "n":
                    return null;
                default:
                    System.out.println("Введите да или нет.");
            }
        }
    }
}
