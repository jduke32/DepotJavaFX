package ru.concerteza.util.io;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.UnhandledException;
import ru.concerteza.util.concurrency.CallableList;
import ru.concerteza.util.io.ru.concerteza.util.io.IOFileFilterPredicate;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.openOutputStream;
import static ru.concerteza.util.string.CtzFormatUtils.format;
import static ru.concerteza.util.concurrency.CtzConcurrencyUtils.runnable;

/**
 * User: alexey
 * Date: Oct 30, 2010
 */
public class CtzIOUtils {
    private static final DeleteDirsOnExitList DELETE_DIRS_ON_EXIT_LIST = new DeleteDirsOnExitList();
    private static final DirectoryPredicate DIRECTORY_PREDICATE = new DirectoryPredicate();

    static {
        Runnable runnable = runnable(DELETE_DIRS_ON_EXIT_LIST);
        Thread thread = new Thread(runnable);
        Runtime.getRuntime().addShutdownHook(thread);
    }

    public static XMLEventReader closeQuietly(XMLEventReader reader) {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (XMLStreamException ioe) {
            // ignore
        }
        return reader;
    }

    public static Connection closeQuietly(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            // ignore
        }
        return conn;
    }

    public static Statement closeQuietly(Statement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            // ignore
        }
        return stmt;
    }

    public static void closeAllQuietly(Closeable... closables) {
        for(Closeable cl : closables) {
            IOUtils.closeQuietly(cl);
        }
    }

    public static File codeSourceDir(Class<?> clazz) {
        try {
            URI uri = clazz.getProtectionDomain().getCodeSource().getLocation().toURI();
            File jarOrDir = new File(uri);
            return jarOrDir.isDirectory() ? jarOrDir : jarOrDir.getParentFile();
        } catch (Exception e) {
            throw new UnhandledException(e);
        }
    }

    public static File appendToFile(File file, String in, String encoding) throws ru.concerteza.util.io.RuntimeIOException {
        InputStream stream = null;
        try {
            stream = IOUtils.toInputStream(in, encoding);
            appendToFile(file, stream);
        } catch (IOException e) {
            throw new ru.concerteza.util.io.RuntimeIOException(e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
        return file;
    }

    public static File appendToFile(File file, InputStream in) throws ru.concerteza.util.io.RuntimeIOException {
        OutputStream stream = null;
        try {
            stream = openOutputStream(file, true);
            IOUtils.copy(in, stream);
        } catch (IOException e) {
            throw new ru.concerteza.util.io.RuntimeIOException(e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
        return file;
    }

    public static File mkdirs(File dir) throws ru.concerteza.util.io.RuntimeIOException {
        if(dir.exists()) {
            if(dir.isFile()) throw new ru.concerteza.util.io.RuntimeIOException(format("Cannot write to directory: '{}'", dir.getAbsolutePath()));
        } else {
            boolean res = dir.mkdirs();
            if(!res) throw new ru.concerteza.util.io.RuntimeIOException(format("Cannot create directory: '{}'", dir.getAbsolutePath()));
        }
        return dir;
    }

    public static File createTmpFile(Class<?> clazz) throws ru.concerteza.util.io.RuntimeIOException {
        try {
            File tmp = File.createTempFile(clazz.getName(), ".tmp");
            tmp.deleteOnExit();
            return tmp;
        } catch (IOException e) {
            throw new ru.concerteza.util.io.RuntimeIOException(e);
        }
    }

    public static File createTmpDir(Class<?> clazz) throws ru.concerteza.util.io.RuntimeIOException {
        try {
            File baseDir = new File(System.getProperty("java.io.tmpdir"));
            String baseName = format("{}_{}.tmp", clazz.getName(), currentTimeMillis());
            File tmp = new File(baseDir, baseName);
            boolean res = tmp.mkdirs();
            if (!res) throw new IOException(format("Cannot create directory: '{}'", tmp.getAbsolutePath()));
            DELETE_DIRS_ON_EXIT_LIST.add(tmp);
            return tmp;
        } catch (IOException e) {
            throw new ru.concerteza.util.io.RuntimeIOException(e);
        }
    }

    public static List<File> listFiles(File dir, boolean includeEmptyDirLeafs) throws ru.concerteza.util.io.RuntimeIOException {
        return ImmutableList.copyOf(iterateFiles(dir, TrueFileFilter.TRUE, TrueFileFilter.TRUE, includeEmptyDirLeafs));
    }

    public static List<File> listFiles(File dir, IOFileFilter fileFilter, boolean includeEmptyDirLeafs) throws ru.concerteza.util.io.RuntimeIOException {
        return ImmutableList.copyOf(iterateFiles(dir, fileFilter, TrueFileFilter.TRUE, includeEmptyDirLeafs));
    }

    public static List<File> listFiles(File dir, IOFileFilter fileFilter, IOFileFilter dirFilter, boolean includeEmptyDirLeafs) throws ru.concerteza.util.io.RuntimeIOException {
        return ImmutableList.copyOf(iterateFiles(dir, fileFilter, dirFilter, includeEmptyDirLeafs));
    }

    public static Iterable<File> iterateFiles(File dir, IOFileFilter fileFilter, IOFileFilter dirFilter, boolean includeEmptyDirLeafs) throws ru.concerteza.util.io.RuntimeIOException {
        Iterable<File> files = new FilesIterable(dir, dirFilter, includeEmptyDirLeafs);
        // dir filter is already applied to empty dir leafs
        Predicate<File> predicate = Predicates.or(IOFileFilterPredicate.of(fileFilter), DIRECTORY_PREDICATE);
        return Iterables.filter(files, predicate);
    }

    // awaiting NIO2
    public static int permissionsOctal(File file) {
        int res = 00;
        if(file.canRead()) res += 04;
        if(file.canWrite()) res += 02;
        if(file.canExecute()) res += 01;
        return res;
    }

    private static class DeleteDirsOnExitList extends CallableList<File> {

        public DeleteDirsOnExitList add(File file) {
            super.add(new DeleteDirCallable(file));
            return this;
        }
    }

    private static class DeleteDirCallable implements Callable<File> {
        private final File dir;

        private DeleteDirCallable(File dir) {
            this.dir = dir;
        }

        @Override
        public File call() throws Exception {
            try {
                FileUtils.deleteDirectory(dir);
            } catch (Exception e){
                // ignore
            }
            return dir;
        }
    }


    private static class FilesIterable implements Iterable<File> {
        private final File dir;
        private final IOFileFilter dirFilter;
        private final boolean includeEmptyDirLeafs;

        private FilesIterable(File dir, IOFileFilter dirFilter, boolean includeEmptyDirLeafs) {
            this.dir = dir;
            this.dirFilter = dirFilter;
            this.includeEmptyDirLeafs = includeEmptyDirLeafs;
        }

        @Override
        public Iterator<File> iterator() {
            return new FilesIterator(dir, dirFilter, includeEmptyDirLeafs);
        }
    }

    private static class FilesIterator extends AbstractIterator<File> {
        private final File dir;
        private final IOFileFilter dirFilter;
        private final boolean includeEmptyDirLeafs;

        private Iterator<File> fileChildren;
        private Iterator<File> dirChildren;
        private Iterator<File> curDirIter = Iterators.emptyIterator();
        private boolean needInit = true;

        private FilesIterator(File dir, IOFileFilter dirFilter, boolean includeEmptyDirLeafs) {
            this.dir = dir;
            this.dirFilter = dirFilter;
            this.includeEmptyDirLeafs = includeEmptyDirLeafs;
        }

        @Override
        protected File computeNext() {
            if(needInit) {
                init();
                needInit = false;
            }
            if(fileChildren.hasNext()) return fileChildren.next();
            if(curDirIter.hasNext()) return curDirIter.next();
            if(dirChildren.hasNext()) {
                curDirIter = new FilesIterator(dirChildren.next(), dirFilter, includeEmptyDirLeafs);
                return computeNext();
            }
            return endOfData();
        }

        private void init() {
            File[] childrenArr = dir.listFiles();
            if(null == childrenArr) throw new ru.concerteza.util.io.RuntimeIOException("Cannot list directory: " + dir.getAbsolutePath());
            if(0 == childrenArr.length) {
                fileChildren = includeEmptyDirLeafs && dirFilter.accept(dir) ? Iterators.forArray(dir) : Iterators.<File>emptyIterator();
                dirChildren = Iterators.emptyIterator();
            } else {
                List<File> children = asList(childrenArr);
                fileChildren = Iterators.filter(children.iterator(), Predicates.not(DIRECTORY_PREDICATE));
                Predicate<File> dirPredicate = Predicates.and(IOFileFilterPredicate.of(dirFilter), DIRECTORY_PREDICATE);
                dirChildren = Iterators.filter(children.iterator(), dirPredicate);
            }
        }
    }

    private static class DirectoryPredicate implements Predicate<File> {
        @Override
        public boolean apply(File input) {
            return input.isDirectory();
        }
    }
}