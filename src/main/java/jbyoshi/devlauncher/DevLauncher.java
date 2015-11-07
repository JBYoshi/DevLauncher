/*
 * Copyright (c) 2015 JBYoshi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jbyoshi.devlauncher;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

import com.mojang.authlib.exceptions.*;

public final class DevLauncher {

	private DevLauncher(JFrame frame, DefaultListModel<Profile> list) {
		this.frame = frame;
		this.list = list;
	}

	public final JFrame frame;
	private final DefaultListModel<Profile> list;

	public static void main(String[] args) {
		GameLauncher.extraArgs = args;
		try {
			LauncherProfiles.load();
		} catch (IOException e) {
			System.err.println("Unable to load online profiles. They will disappear for this session.");
			try {
				Thread.sleep(2500);
			} catch (InterruptedException e1) {
			}
		}

		final JFrame frame = new JFrame("Minecraft Dev Launcher");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		final DefaultListModel<Profile> list = new DefaultListModel<>();
		for (OnlineProfile prof : LauncherProfiles.INSTANCE.getProfiles()) {
			list.addElement(prof);
		}
		for (OfflineProfile prof : OfflineProfile.profiles) {
			list.addElement(prof);
		}
		final JList<Profile> listComp = new JList<Profile>(list);
		final DevLauncher access = new DevLauncher(frame, list);
		listComp.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() >= 2) {
					int index = listComp.locationToIndex(evt.getPoint());
					Profile prof = list.getElementAt(index);
					access.start(prof);
				}
			}
		});
		frame.add(new JScrollPane(listComp), BorderLayout.CENTER);

		final JComponent toolbar = Box.createHorizontalBox();
		toolbar.add(new JButton(new AbstractAction("Add Account") {
			private static final long serialVersionUID = -5623034216158220523L;

			@Override
			public void actionPerformed(ActionEvent event) {
				access.showLoginDialog();
			}
		}));
		toolbar.add(new JButton(new AbstractAction("Add Fake User") {
			private static final long serialVersionUID = -4307781537734262808L;

			@Override
			public void actionPerformed(ActionEvent event) {
				String username = "?";
				while (username != null && !OfflineProfile.isValidUsername(username)) {
					username = JOptionPane
							.showInputDialog(frame,
									new String[] { "Please enter a username. It must be between 3 and",
											"16 characters long, and may only contain",
							"letters, numbers, and underscores." });
				}
				if (username != null) {
					list.addElement(new OfflineProfile(username));
				}
			}
		}));
		toolbar.add(new JButton(new AbstractAction("Remove User") {
			private static final long serialVersionUID = -629214285966436990L;

			@Override
			public void actionPerformed(ActionEvent event) {
				if (listComp.getSelectedIndex() >= 0) {
					list.remove(listComp.getSelectedIndex()).onRemoved();
				}
			}
		}));
		toolbar.add(new JButton(new AbstractAction("Start Server") {
			private static final long serialVersionUID = -629214285966436990L;

			@Override
			public void actionPerformed(ActionEvent event) {
				frame.dispose();
				GameLauncher.launchServer();
			}
		}));
		frame.add(toolbar, BorderLayout.NORTH);

		frame.pack();
		frame.setVisible(true);
	}

	public static final File workingDirectory;

	static {
		String os = System.getProperty("os.name").toLowerCase();
		String userHome = System.getProperty("user.home", ".");
		if (os.contains("win")) {
			String applicationData = System.getenv("APPDATA");
			String folder = applicationData != null ? applicationData : userHome;

			workingDirectory = new File(folder, ".minecraft/");
		} else if (os.contains("mac")) {
			workingDirectory = new File(userHome, "Library/Application Support/minecraft");
		} else if (os.contains("linux") || os.contains("unix")) {
			workingDirectory = new File(userHome, ".minecraft/");
		} else {
			workingDirectory = new File(userHome, "minecraft/");
		}
	}

	public void start(Profile prof) {
		try {
			prof.validate();
			frame.dispose();
			GameLauncher.launchClient(prof);
		} catch (LaunchException e) {
			if (frame.isVisible()) {
				e.handle(this);
			} else {
				e.printStackTrace();
			}
			list.clear();
			for (OnlineProfile prof1 : LauncherProfiles.INSTANCE.getProfiles()) {
				list.addElement(prof1);
			}
			for (OfflineProfile prof1 : OfflineProfile.profiles) {
				list.addElement(prof1);
			}
		}
	}

	public void showLoginDialog() {
		while (true) {
			try {
				OnlineProfile profile = OnlineProfile.logIn(frame);
				if (profile != null) {
					list.addElement(profile);
				}
				return;
			} catch (UserMigratedException e) {
				JOptionPane.showMessageDialog(frame, "Please use your email address to log in.", "Error",
						JOptionPane.ERROR_MESSAGE);
			} catch (InvalidCredentialsException e) {
				JOptionPane.showMessageDialog(frame,
						new Object[] { "Invalid username or password. Please check your credentials again.",
				"If you have a legacy account (you log in with your username), use your username instead of your email." },
						"Error", JOptionPane.ERROR_MESSAGE);
			} catch (AuthenticationUnavailableException e) {
				JOptionPane.showMessageDialog(frame,
						new Object[] { "Looks like the authentication servers are down. Try again later.", e }, "Error",
						JOptionPane.ERROR_MESSAGE);
			} catch (AuthenticationException e) {
				JOptionPane.showMessageDialog(frame, new Object[] { "An unexpected error occurred.", e }, "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

}
