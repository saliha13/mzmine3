/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine 3.
 *
 * MZmine 3 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine 3 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine 3; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.gui.mainwindow;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.gui.NewVersionCheck;
import io.github.mzmine.gui.NewVersionCheck.CheckType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.io.projectload.ProjectOpeningTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * The controller class for MainMenu.fxml
 *
 */
public class MainMenuController {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  @FXML
  private Menu windowsMenu, recentProjectsMenu;

  public void closeProject(Event event) {
    MZmineGUI.requestCloseProject();
  }

  public void exitApplication(Event event) {
    MZmineGUI.requestQuit();
  }

  public void openLink(Event event) {

    assert event.getSource() instanceof MenuItem;
    final MenuItem menuItem = (MenuItem) event.getSource();
    assert menuItem.getUserData() instanceof String;
    final String linkURL = (String) menuItem.getUserData();
    assert linkURL != null;

    // Open link in browser
    if (!Desktop.isDesktopSupported()) {
      logger.severe("Could not open browser, Desktop support is not available");
      return;
    }

    try {
      Desktop desktop = Desktop.getDesktop();
      desktop.browse(new URI(linkURL));
    } catch (IOException | URISyntaxException e) {
      e.printStackTrace();
    }

  }

  public void versionCheck(Event event) {
    // Check for new version of MZmine
    logger.info("Checking for new MZmine version");
    NewVersionCheck NVC = new NewVersionCheck(CheckType.MENU);
    Thread nvcThread = new Thread(NVC);
    nvcThread.setPriority(Thread.MIN_PRIORITY);
    nvcThread.start();
  }

  public void setPreferences(Event event) {
    // Show the Preferences dialog
    logger.info("Showing the Preferences dialog");
    // MZmineCore.getConfiguration().getPreferences().showSetupDialog(null);
  }

  public void showAbout(Event event) {
    MZmineGUI.showAboutWindow();
  }

  public void fillWindowsMenu(Event event) {
    final var windowsMenuItems = windowsMenu.getItems();
    while (windowsMenuItems.size() > 2)
      windowsMenuItems.remove(2);
    for (Window win : Window.getWindows()) {
      if (win instanceof Stage) {
        Stage stage = (Stage) win;
        final MenuItem item = new MenuItem(stage.getTitle());
        windowsMenuItems.add(item);
      }
    }
  }

  public void closeAllWindows(Event event) {
    for (Window win : Window.getWindows()) {
      if (win == MZmineCore.getDesktop().getMainWindow())
        continue;
      win.hide();
    }

  }

  @SuppressWarnings("unchecked")
  public void runModule(Event event) {
    assert event.getSource() instanceof MenuItem;
    final MenuItem menuItem = (MenuItem) event.getSource();
    assert menuItem.getUserData() instanceof String;
    final String moduleClass = (String) menuItem.getUserData();
    assert moduleClass != null;

    logger.info("Menu item activated for module " + moduleClass);
    Class<? extends MZmineRunnableModule> moduleJavaClass;
    try {
      moduleJavaClass = (Class<? extends MZmineRunnableModule>) Class.forName(moduleClass);
    } catch (Throwable e) {
      MZmineCore.getDesktop().displayMessage("Cannot load module class " + moduleClass);
      return;
    }

    MZmineModule module = MZmineCore.getModuleInstance(moduleJavaClass);

    if (module == null) {
      MZmineCore.getDesktop().displayMessage("Cannot find module of class " + moduleClass);
      return;
    }

    ParameterSet moduleParameters =
        MZmineCore.getConfiguration().getModuleParameters(moduleJavaClass);

    logger.info("Setting parameters for module " + module.getName());

    SwingUtilities.invokeLater(() -> {
      ExitCode exitCode = moduleParameters.showSetupDialog(null, true);
      if (exitCode != ExitCode.OK)
        return;

      ParameterSet parametersCopy = moduleParameters.cloneParameterSet();
      logger.finest("Starting module " + module.getName() + " with parameters " + parametersCopy);
      MZmineCore.runMZmineModule(moduleJavaClass, parametersCopy);
    });
  }

  public void fillRecentProjects(Event event) {

    recentProjectsMenu.getItems().clear();

    var recentProjects = MZmineCore.getConfiguration().getLastProjectsParameter().getValue();

    if ((recentProjects == null) || (recentProjects.isEmpty())) {
      recentProjectsMenu.setDisable(true);
      return;
    }

    recentProjectsMenu.setDisable(false);

    // add items to load last used projects directly
    recentProjects.stream().map(File::getAbsolutePath).forEach(name -> {
      MenuItem item = new MenuItem(name);

      item.setOnAction(e -> {
        MenuItem c = (MenuItem) e.getSource();
        if (c == null)
          return;
        File f = new File(c.getText());
        if (f.exists()) {
          // load file
          ProjectOpeningTask newTask = new ProjectOpeningTask(f);
          MZmineCore.getTaskController().addTask(newTask);
        }
      });
      recentProjectsMenu.getItems().add(item);
    });
  }
}
