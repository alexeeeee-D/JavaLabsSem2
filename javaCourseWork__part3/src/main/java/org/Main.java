package org;

import service.DataService;

public class Main {

    public static void main(String[] args) {
        DataService dataService = new DataService();
        ConsoleUI ui = new ConsoleUI(dataService);
        if (args.length == 0) {
            ui.runInteractiveMode();
        } else {
            ui.runAutoMode(args);
        }
    }
}
