package com.threerings.media.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * Creates a file that lists all the resources in a fileset out to an index file.
 */
public class ResourceIndexerTask extends Task
{
    /**
     * Adds a nested &lt;fileset&gt; element.
     */
    public void addFileset (FileSet set)
    {
        _filesets.add(set);
    }
    
    public void setIndexFile (String file)
    {
        _indexFile = file;
    }
    
    @Override
    public void execute () throws BuildException
    {
        PrintWriter fout = null;
        try {
            fout = new PrintWriter(new FileWriter(_indexFile));
            
            for (FileSet fs : _filesets) {
                DirectoryScanner ds = fs.getDirectoryScanner(getProject());
                String[] srcFiles = ds.getIncludedFiles();
                for (String filename : srcFiles) {
                    fout.println(filename);
                }
            }
            
        } catch (IOException ioe) {
            throw new BuildException(ioe);
        } finally {
            if (fout != null) {
                fout.close();
            }
        }
    }

    /** A list of filesets that contain files to include in the index. */
    protected ArrayList<FileSet> _filesets = new ArrayList<FileSet>();
    
    /** The name of the file to which we should write the index. */
    protected String _indexFile;
}
