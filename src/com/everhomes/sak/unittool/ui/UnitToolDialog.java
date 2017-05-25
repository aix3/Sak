package com.everhomes.sak.unittool.ui;

import com.everhomes.sak.util.PsiUtil;
import com.everhomes.sak.util.Util;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;

import javax.swing.*;
import java.awt.event.*;

public class UnitToolDialog extends JDialog {

    private final int INIT_W = 600; //窗体初始宽度
    private final int INIT_H = 460; //窗体初始高度

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JButton button1;
    private JButton button2;
    private JButton button3;

    Project project;

    public UnitToolDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        button1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onButton1();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);


        setLocationRelativeTo(null);
    }

    private void onButton1() {
        PsiClass psiClass = Util.chooseClass(project, null);
    }

    private void onButton2() {
        PsiDirectory psiClass = PsiUtil.chooseDirectory(project);
    }

    private void onOK() {
        // add your code here
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    public static void main(String[] args) {
        UnitToolDialog dialog = new UnitToolDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
