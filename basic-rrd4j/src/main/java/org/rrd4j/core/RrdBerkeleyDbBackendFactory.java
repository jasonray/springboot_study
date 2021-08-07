package org.rrd4j.core;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

/**
 * {@link org.rrd4j.core.RrdBackendFactory} that uses
 * <a href="http://www.oracle.com/technetwork/database/berkeleydb/overview/index.html">Oracle Berkeley DB Java Edition</a>
 * to read data. Construct a BerkeleyDB {@link com.sleepycat.je.Database} object and pass it via the constructor.
 *
 * @author <a href="mailto:m.bogaert@memenco.com">Mathias Bogaert</a>
 */
@RrdBackendAnnotation(name="BERKELEY", shouldValidateHeader=false)
public class RrdBerkeleyDbBackendFactory extends RrdBackendFactory {

    private final Database rrdDatabase;
    private final Set<String> pathCache = new CopyOnWriteArraySet<>();

    /**
     * <p>Constructor for RrdBerkeleyDbBackendFactory.</p>
     *
     * @param rrdDatabase a {@link com.sleepycat.je.Database} object.
     */
    public RrdBerkeleyDbBackendFactory(Database rrdDatabase) {
        this.rrdDatabase = rrdDatabase;
    }

    /**
     * {@inheritDoc}
     *
     * Creates new RrdBerkeleyDbBackend object for the given id (path).
     */
    protected RrdBackend open(String path, boolean readOnly) throws IOException {
        if (pathCache.contains(path)) {
            DatabaseEntry theKey = new DatabaseEntry(path.getBytes(StandardCharsets.UTF_8));
            DatabaseEntry theData = new DatabaseEntry();

            try {
                rrdDatabase.get(null, theKey, theData, LockMode.DEFAULT);
            } catch (DatabaseException de) {
                throw new RrdBackendException("BerkeleyDB DatabaseException on " + path + "; " + de.getMessage(), de);
            }

            return new RrdBerkeleyDbBackend(theData.getData(), path, rrdDatabase);
        }
        else {
            return new RrdBerkeleyDbBackend(path, rrdDatabase);
        }
    }

    @Override
    public URI getUri(String path) {
        try {
            return new URI("berkeley", "", rrdDatabase.getEnvironment().getHome().getAbsolutePath(), rrdDatabase.getDatabaseName(), path);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("can't extract URI from '" + path + "': " + e.getMessage(), e);
        }
    }

    @Override
    public String getPath(URI uri) {
        return uri.getFragment();
    }

    @Override
    public boolean canStore(URI uri) {
        if (! "berkeley".equals(uri.getScheme())) {
            return false;
        } else if ( !uri.getPath().isEmpty() && !uri.getPath().equals(rrdDatabase.getEnvironment().getHome().getAbsolutePath())) {
            return false;
        } else if ( !uri.getQuery().isEmpty() && !uri.getQuery().equals(rrdDatabase.getDatabaseName())) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * <p>delete.</p>
     *
     * @param path a {@link java.lang.String} object.
     */
    public void delete(String path) {
        try {
            rrdDatabase.delete(null, new DatabaseEntry(path.getBytes(StandardCharsets.UTF_8)));
        } catch (DatabaseException de) {
            throw new RuntimeException(de.getMessage(), de);
        }
        pathCache.remove(path);
    }

    /**
     * {@inheritDoc}
     *
     * Checks if the RRD with the given id (path) already exists in the database.
     */
    protected boolean exists(String path) throws IOException {
        if (pathCache.contains(path)) {
            return true;
        } else {
            DatabaseEntry theKey = new DatabaseEntry(path.getBytes(StandardCharsets.UTF_8));

            DatabaseEntry theData = new DatabaseEntry();
            theData.setPartial(0, 0, true); // avoid returning rrd data since we're only checking for existence

            try {
                boolean pathExists = rrdDatabase.get(null, theKey, theData, LockMode.DEFAULT) == OperationStatus.SUCCESS;
                if (pathExists) {
                    pathCache.add(path);
                }
                return pathExists;
            } catch (DatabaseException de) {
                throw new RrdBackendException("BerkeleyDB DatabaseException on " + path + "; " + de.getMessage(), de);
            }
        }
    }

    @Override
    public void close() throws IOException {
        rrdDatabase.close();
    }

}
