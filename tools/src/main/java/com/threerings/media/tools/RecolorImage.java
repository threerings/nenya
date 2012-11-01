//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// https://github.com/threerings/nenya
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.media.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import com.google.common.collect.Lists;

import com.samskivert.util.PrefsConfig;
import com.samskivert.util.QuickSort;

import com.samskivert.swing.HGroupLayout;
import com.samskivert.swing.VGroupLayout;
import com.samskivert.swing.util.SwingUtil;

import com.threerings.media.image.ColorPository;
import com.threerings.media.image.Colorization;
import com.threerings.media.image.ImageUtil;
import com.threerings.media.image.ColorPository.ClassRecord;
import com.threerings.media.image.ColorPository.ColorRecord;
import com.threerings.media.image.tools.xml.ColorPositoryParser;

/**
 * Tests the image recoloring code.
 */
public class RecolorImage extends JPanel
    implements ActionListener
{
    public RecolorImage ()
    {
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        VGroupLayout vlay = new VGroupLayout(VGroupLayout.STRETCH);
        vlay.setOffAxisPolicy(VGroupLayout.STRETCH);
        setLayout(vlay);

        JPanel images = new JPanel(new HGroupLayout());
        images.add(_oldImage = new JLabel());
        images.add(_newImage = new JLabel());
        add(new JScrollPane(images));

        // Image file
        JPanel file = new JPanel(new HGroupLayout(HGroupLayout.STRETCH));
        file.add(new JLabel("Image file:"), HGroupLayout.FIXED);
        file.add(_imagePath = new JTextField());
        _imagePath.setEditable(false);
        JButton browse = new JButton("Browse...");
        browse.setActionCommand(BROWSE_FOR_IMAGE_FILE);
        browse.addActionListener(this);
        file.add(browse, HGroupLayout.FIXED);
        JButton reload = new JButton("Reload");
        reload.setActionCommand(RELOAD_IMAGE);
        reload.addActionListener(this);
        file.add(reload, HGroupLayout.FIXED);
        add(file, VGroupLayout.FIXED);

        JPanel colFile = new JPanel(new HGroupLayout(HGroupLayout.STRETCH));
        colFile.add(new JLabel("Colorize file:"), HGroupLayout.FIXED);
        colFile.add(_colFilePath = new JTextField());
        _colFilePath.setEditable(false);
        browse = new JButton("Browse...");
        browse.setActionCommand(BROWSE_FOR_COLORIZATION_FILE);
        browse.addActionListener(this);
        colFile.add(browse, HGroupLayout.FIXED);
        add(colFile, VGroupLayout.FIXED);

        _tabs = new JTabbedPane();
        _tabs.addChangeListener(new ChangeListener() {
            public void stateChanged (ChangeEvent event) {
                convert();
            }
        });
        add(_tabs, VGroupLayout.FIXED);

        // Colorization file
        JPanel byFile = new JPanel(new VGroupLayout(VGroupLayout.STRETCH));
        _tabs.addTab("Using Color Class", byFile);

        byFile.add(_classList = new JComboBox());
        byFile.add(_labelColors = new JCheckBox("Label Colorizations"), VGroupLayout.FIXED);
        ActionListener al = new ActionListener() {
            public void actionPerformed (ActionEvent ae) {
                convert();
            }
        };
        _classList.addActionListener(al);

        // Specific colors
        JPanel multiColor = new JPanel(new GridLayout(4, 2));
        _tabs.addTab("Multi-Color", multiColor);
        multiColor.add(_classList1 = new JComboBox());
        multiColor.add(_colorList1 = new JComboBox());
        multiColor.add(_classList2 = new JComboBox());
        multiColor.add(_colorList2 = new JComboBox());
        multiColor.add(_classList3 = new JComboBox());
        multiColor.add(_colorList3 = new JComboBox());
        multiColor.add(_classList4 = new JComboBox());
        multiColor.add(_colorList4 = new JComboBox());

        _colorList1.addActionListener(al);
        _colorList2.addActionListener(al);
        _colorList3.addActionListener(al);
        _colorList4.addActionListener(al);

        _labelColors.addActionListener(al);
        JPanel specRecolor = new JPanel(new VGroupLayout(VGroupLayout.STRETCH));
        _tabs.addTab("Manual Recolor", specRecolor);
        JPanel controls = new JPanel(new HGroupLayout(HGroupLayout.STRETCH));
        controls.add(new JLabel("Source color:"), HGroupLayout.FIXED);
        controls.add(_source = new JTextField("FF0000"));
        _colorLabel = new JPanel();
        _colorLabel.setSize(48, 48);
        _colorLabel.setOpaque(true);
        controls.add(_colorLabel, HGroupLayout.FIXED);
        controls.add(new JLabel("Target color:"), HGroupLayout.FIXED);
        controls.add(_target = new JTextField());
        JButton update = new JButton("Update");
        update.setActionCommand(UPDATE_TARGET_COLOR);
        update.addActionListener(this);
        controls.add(update, HGroupLayout.FIXED);
        specRecolor.add(controls, VGroupLayout.FIXED);

        HGroupLayout hlay = new HGroupLayout(HGroupLayout.STRETCH);
        JPanel dists = new JPanel(hlay);
        dists.add(new JLabel("HSV distances:"), HGroupLayout.FIXED);
        dists.add(_hueD = new SliderAndLabel(0.0f, 1.0f, 0.05f));
        dists.add(_saturationD = new SliderAndLabel(0.0f, 1.0f, 0.8f));
        dists.add(_valueD = new SliderAndLabel(0.0f, 1.0f, 0.6f));
        specRecolor.add(dists, VGroupLayout.FIXED);

        hlay = new HGroupLayout(HGroupLayout.STRETCH);
        JPanel offsets = new JPanel(hlay);
        offsets.add(new JLabel("HSV offsets:"), HGroupLayout.FIXED);
        offsets.add(_hueO = new SliderAndLabel(-1.0f, 1.0f, 0.1f));
        offsets.add(_saturationO = new SliderAndLabel(-1.0f, 1.0f, 0.0f));
        offsets.add(_valueO = new SliderAndLabel(-1.0f, 1.0f, 0.0f));
        specRecolor.add(offsets, VGroupLayout.FIXED);

        add(_status = new JTextField(), VGroupLayout.FIXED);
        _status.setEditable(false);

        hlay = new HGroupLayout();
        hlay.setJustification(HGroupLayout.CENTER);
        JPanel buttons = new JPanel(hlay);
        JButton button = new JButton("Convert");
        button.setActionCommand(CONVERT);
        button.addActionListener(this);
        buttons.add(button);
        button = new JButton("Save Snapshot");
        button.setActionCommand(SAVE_COLORIZED_IMAGE);
        button.addActionListener(this);
        buttons.add(button);
        add(buttons, VGroupLayout.FIXED);

        // listen for mouse clicks
        images.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed (MouseEvent event) {
                RecolorImage.this.mousePressed(event);
            }
        });

        // we'll be using a file chooser
        String cwd = System.getProperty("user.dir");

        String image = CONFIG.getValue(LAST_IMAGE_KEY, cwd);
        String colorization = CONFIG.getValue(LAST_COLORIZATION_KEY, cwd);
        _chooser = new JFileChooser(image);
        _colChooser = new JFileChooser(colorization);
    }

    /**
     * Performs colorizations.
     */
    protected void convert ()
    {
        if (_image == null) {
            return;
        }

        // obtain the target color and offset
        try {
            BufferedImage image;
            if (_tabs.getSelectedIndex() == 0) {
                // All recolorings from file.
                image = getAllRecolors(_labelColors.isSelected());
                if (image == null) {
                    return;
                }
            } else if (_tabs.getSelectedIndex() == 1) {
                ArrayList<Colorization> zations = Lists.newArrayList();
                if (_classList1.getSelectedItem() != NONE) {
                    zations.add(_colRepo.getColorization((String)_classList1.getSelectedItem(),
                        (String)_colorList1.getSelectedItem()));
                }
                if (_classList2.getSelectedItem() != NONE) {
                    zations.add(_colRepo.getColorization((String)_classList2.getSelectedItem(),
                        (String)_colorList2.getSelectedItem()));
                }
                if (_classList3.getSelectedItem() != NONE) {
                    zations.add(_colRepo.getColorization((String)_classList3.getSelectedItem(),
                        (String)_colorList3.getSelectedItem()));
                }
                if (_classList4.getSelectedItem() != NONE) {
                    zations.add(_colRepo.getColorization((String)_classList4.getSelectedItem(),
                        (String)_colorList4.getSelectedItem()));
                }

                image = ImageUtil.recolorImage(_image,
                    zations.toArray(new Colorization[zations.size()]));
            } else {
                // Manual recoloring
                int color = Integer.parseInt(_source.getText(), 16);

                float hueD = _hueD.getValue();
                float satD = _saturationD.getValue();
                float valD = _valueD.getValue();
                float[] dists = new float[] { hueD, satD, valD };

                float hueO = _hueO.getValue();
                float satO = _saturationO.getValue();
                float valO = _valueO.getValue();
                float[] offsets = new float[] { hueO, satO, valO };

                image = ImageUtil.recolorImage(_image, new Color(color), dists, offsets);
            }
            _newImage.setIcon(new ImageIcon(image));
            _status.setText("Recolored image.");
            repaint();

        } catch (NumberFormatException nfe) {
            _status.setText("Invalid value: " + nfe.getMessage());
        }
    }

    /**
     * Gets an image with all recolorings of the selection colorization class.
     */
    public BufferedImage getAllRecolors (boolean label)
    {
        if (_colRepo == null) {
            return null;
        }

        ColorPository.ClassRecord colClass =
            _colRepo.getClassRecord((String)_classList.getSelectedItem());
        int classId = colClass.classId;

        BufferedImage img = new BufferedImage(_image.getWidth(),
            _image.getHeight()*colClass.colors.size(), BufferedImage.TYPE_INT_ARGB);
        Graphics gfx = img.getGraphics();
        gfx.setColor(Color.BLACK);
        int y = 0;


        Integer[] sortedKeys =
            colClass.colors.keySet().toArray(new Integer[colClass.colors.size()]);
        Arrays.sort(sortedKeys);

        for (int key : sortedKeys) {
            Colorization coloriz = _colRepo.getColorization(classId, key);
            BufferedImage subImg = ImageUtil.recolorImage(
                    _image, coloriz.rootColor, coloriz.range, coloriz.offsets);

            gfx.drawImage(subImg, 0, y, null, null);

            if (label) {
                ColorRecord crec = _colRepo.getColorRecord(classId, key);
                gfx.drawString(crec.name, 2, y + gfx.getFontMetrics().getHeight() + 2);
                gfx.drawRect(0, y, _image.getWidth() - 1, _image.getHeight());
            }

            y += subImg.getHeight();
        }

        return img;
    }

    public void actionPerformed (ActionEvent event)
    {
        String cmd = event.getActionCommand();

        if (cmd.equals(CONVERT)) {
            convert();
        } else if (cmd.equals(UPDATE_TARGET_COLOR)) {
            // obtain the target color and offset
            try {
                int source = Integer.parseInt(_source.getText(), 16);
                int target = Integer.parseInt(_target.getText(), 16);
                float[] shsv = rgbToHSV(source);
                float[] thsv = rgbToHSV(target);

                // set the offsets based on the differences
                _hueO.setValue(thsv[0] - shsv[0]);
                _saturationO.setValue(thsv[1] - shsv[1]);
                _valueO.setValue(thsv[2] - shsv[2]);

            } catch (NumberFormatException nfe) {
                _status.setText("Invalid value: " + nfe.getMessage());
            }

        } else if (cmd.equals(BROWSE_FOR_IMAGE_FILE)) {
            int result = _chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                setImage(_chooser.getSelectedFile());
            }
        } else if (cmd.equals(RELOAD_IMAGE)) {
            setImage(_chooser.getSelectedFile());
        } else if (cmd.equals(BROWSE_FOR_COLORIZATION_FILE)) {
            int result = _colChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                setColorizeFile(_colChooser.getSelectedFile());
            }
        } else if (cmd.equals(SAVE_COLORIZED_IMAGE)) {
            JFileChooser chooser = new JFileChooser(_chooser.getSelectedFile());
            chooser.setFileFilter(new FileFilter() {
                @Override public boolean accept (File f) {
                    return (f.isDirectory() || f.getName().endsWith(".png"));
                }
                @Override public String getDescription () {
                    return "PNG Files";
                }
            });
            int result = chooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                try {
                    ImageIO.write((BufferedImage)((ImageIcon)_newImage.getIcon()).getImage(),
                        "png", chooser.getSelectedFile());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this,
                        "Error while saving image: " + e.getMessage(),
                        "Error Saving Image",
                         JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    public void setImage (File path)
    {
        try {
            _image = ImageIO.read(path);
            _imagePath.setText(path.getPath());
            _oldImage.setIcon(new ImageIcon(_image));

            CONFIG.setValue(LAST_IMAGE_KEY, path.getAbsolutePath());
        } catch (IOException ioe) {
            _status.setText("Error opening image file: " + ioe);
        }
    }

    public void setupColors (JComboBox box, String className)
    {
        box.removeAllItems();
        if (className != null && className != NONE) {
            ClassRecord classRec = _colRepo.getClassRecord(className);

            ArrayList<String> colorNames = Lists.newArrayList();
            for (ColorRecord color : classRec.colors.values()) {
                colorNames.add(color.name);
            }

            QuickSort.sort(colorNames);

            for (String colorName : colorNames) {
                box.addItem(colorName);
            }
        }
    }

    /**
     * Loads up the colorization classes from the specified file.
     */
    public void setColorizeFile (File path)
    {
        try {
            if (path.getName().endsWith("xml")) {
                ColorPositoryParser parser = new ColorPositoryParser();
                _colRepo = (ColorPository)(parser.parseConfig(path));
            } else {
                _colRepo = ColorPository.loadColorPository(new FileInputStream(path));
            }

            _classList.removeAllItems();
            _classList1.removeAllItems();
            _classList1.addActionListener(new ActionListener() {
                public void actionPerformed (ActionEvent event) {
                    setupColors(_colorList1, (String)_classList1.getSelectedItem());
                }
            });
            _classList2.removeAllItems();
            _classList2.addActionListener(new ActionListener() {
                public void actionPerformed (ActionEvent event) {
                    setupColors(_colorList2, (String)_classList2.getSelectedItem());
                }
            });
            _classList3.removeAllItems();
            _classList3.addActionListener(new ActionListener() {
                public void actionPerformed (ActionEvent event) {
                    setupColors(_colorList3, (String)_classList3.getSelectedItem());
                }
            });
            _classList4.removeAllItems();
            _classList4.addActionListener(new ActionListener() {
                public void actionPerformed (ActionEvent event) {
                    setupColors(_colorList4, (String)_classList4.getSelectedItem());
                }
            });
            Iterator<ColorPository.ClassRecord> iter = _colRepo.enumerateClasses();
            ArrayList<String> names = Lists.newArrayList();
            while (iter.hasNext()) {
                String str = iter.next().name;
                names.add(str);
            }

            _classList1.addItem(NONE);
            _classList2.addItem(NONE);
            _classList3.addItem(NONE);
            _classList4.addItem(NONE);

            Collections.sort(names);
            for (String name : names) {
                _classList.addItem(name);
                _classList1.addItem(name);
                _classList2.addItem(name);
                _classList3.addItem(name);
                _classList4.addItem(name);
            }

            _classList.setSelectedIndex(0);
            _classList1.setSelectedIndex(0);
            _classList2.setSelectedIndex(0);
            _classList3.setSelectedIndex(0);
            _classList4.setSelectedIndex(0);
            _colFilePath.setText(path.getPath());

            CONFIG.setValue(LAST_COLORIZATION_KEY, path.getAbsolutePath());
        }  catch (Exception ex) {
            _status.setText("Error opening colorization file: " + ex);
        }
    }

    protected static float[] rgbToHSV (int rgb)
    {
        float[] hsv = new float[3];
        Color color = new Color(rgb);
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsv);
        return hsv;
    }

    public void mousePressed (MouseEvent event)
    {
        // if the click was in the bounds of the source image, grab the
        // pixel color and use that to set the "source" color
        int x = event.getX(), y = event.getY();
        Rectangle ibounds = _oldImage.getBounds();
        if (ibounds.contains(x, y)) {
            int argb = _image.getRGB(x - ibounds.x, y - ibounds.y);
            String cstr = Integer.toString(argb & 0xFFFFFF, 16);
            _source.setText(cstr.toUpperCase());
            _colorLabel.setBackground(new Color(argb));
            _colorLabel.repaint();
        }
    }

    /**
     * Class with linked slider and label arranged vertically.
     */
    protected class SliderAndLabel extends JPanel
    {
        public SliderAndLabel (float minf, float maxf, float valuef) {
            int min = (int)(minf*CONVERSION);
            int max = (int)(maxf*CONVERSION);
            int value = (int)(valuef*CONVERSION);
            setLayout(new VGroupLayout(VGroupLayout.STRETCH));
            _intField = new JLabel(String.valueOf(value/CONVERSION));
            _slider = new JSlider(min, max, value);

            _slider.addChangeListener(new ChangeListener() {
                public void stateChanged (ChangeEvent ce) {
                    _intField.setText(String.valueOf((_slider.getValue())/CONVERSION));

                    convert();
                }
            });
            add(_intField);
            add(_slider);
        }

        public float getValue () {
            return _slider.getValue()/CONVERSION;
        }

        public void setValue (float val) {
            _slider.setValue((int)(val*CONVERSION));
        }

        protected JSlider _slider;
        protected JLabel _intField;

        protected final static float CONVERSION = 1000.0f;
    }

    public static void main (String[] args)
    {
        try {
            JFrame frame = new JFrame("Image recoloring test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            RecolorImage panel = new RecolorImage();

            // load up the image from the command line if one was
            // specified
            if (args.length > 0) {
                panel.setImage(new File(args[0]));
            }

            frame.getContentPane().add(panel, BorderLayout.CENTER);
            frame.setSize(600, 600);
            SwingUtil.centerWindow(frame);
            frame.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    protected BufferedImage _image;
    protected JFileChooser _chooser;
    protected JFileChooser _colChooser;
    protected JTextField _imagePath;
    protected JTextField _colFilePath;

    protected JLabel _oldImage;
    protected JLabel _newImage;
    protected JPanel _colorLabel;

    protected JTextField _source;
    protected JTextField _target;

    protected SliderAndLabel _hueO;
    protected SliderAndLabel _saturationO;
    protected SliderAndLabel _valueO;

    protected SliderAndLabel _hueD;
    protected SliderAndLabel _saturationD;
    protected SliderAndLabel _valueD;

    protected JTextField _status;

    protected JComboBox _classList;
    protected JComboBox _classList1;
    protected JComboBox _classList2;
    protected JComboBox _classList3;
    protected JComboBox _classList4;
    protected JComboBox _colorList1;
    protected JComboBox _colorList2;
    protected JComboBox _colorList3;
    protected JComboBox _colorList4;
    protected JCheckBox _labelColors;

    protected JTabbedPane _tabs;

    protected ColorPository _colRepo;

    protected static final String IMAGE_PATH =
        "bundles/components/pirate/head/regular/standing.png";

    /** The actions for our various buttons. */
    protected static final String BROWSE_FOR_IMAGE_FILE = "browse_image";
    protected static final String RELOAD_IMAGE = "reload_image";
    protected static final String BROWSE_FOR_COLORIZATION_FILE = "browse_colorize";
    protected static final String SAVE_COLORIZED_IMAGE = "save_colorized";
    protected static final String UPDATE_TARGET_COLOR = "update_target";
    protected static final String CONVERT = "convert";

    /** Where we can stash our preferences. */
    protected static final PrefsConfig CONFIG =
        new PrefsConfig("rsrc/config/threerings/recolorimage");

    protected static final String NONE = "<none>";

    /** Keys for our preferences. */
    protected static final String LAST_IMAGE_KEY = "last_image";
    protected static final String LAST_COLORIZATION_KEY = "last_colorization";
}
