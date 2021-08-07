package org.rrd4j.graph;

import static org.rrd4j.ConsolFun.AVERAGE;
import static org.rrd4j.ConsolFun.MAX;
import static org.rrd4j.DsType.GAUGE;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.GregorianCalendar;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.ConsolFun;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.RrdDefTemplate;
import org.rrd4j.core.RrdRandomAccessFileBackendFactory;
import org.rrd4j.data.Variable;
import org.xml.sax.InputSource;

public class TemplateTest {

    static private RrdBackendFactory previousBackend;

    @BeforeClass
    public static void setBackendBefore() {
        previousBackend = RrdBackendFactory.getDefaultFactory();
        RrdBackendFactory.setActiveFactories(new RrdRandomAccessFileBackendFactory());
    }

    @AfterClass
    public static void setBackendAfter() {
        RrdBackendFactory.setActiveFactories(previousBackend);
    }

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @SuppressWarnings("deprecation")
    @Test
    @Ignore("Missing XML export of graph def")
    public void test1() throws IOException {
        RrdGraphDef gdef = new RrdGraphDef();
        gdef.datasource("ds1", "some.rrd", "ds1", ConsolFun.AVERAGE, "file");
        gdef.datasource("ds2", "ds1");
        gdef.datasource("ds3", "ds1", ConsolFun.AVERAGE.getVariable());
        gdef.datasource("ds4", "ds1", new Variable.PERCENTILE(95));
        gdef.line("ds4", Color.BLACK);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.flush();
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        @SuppressWarnings("unused")
        RrdGraphDefTemplate template = new RrdGraphDefTemplate(new InputSource(in));
    }

    @Test
    public void test2() throws IOException {
        InputStream in = getClass().getResourceAsStream("/rrd_graph_def.xml"); 
        RrdGraphDefTemplate template = new RrdGraphDefTemplate(new InputSource(in));
        RrdGraphDef gdef = template.getRrdGraphDef();
        Assert.assertEquals(Duration.ofDays(2).get(ChronoUnit.SECONDS), gdef.getEndTime() - gdef.getStartTime());
    }

    @Test
    public void test_variable() throws IOException {
        InputStream in = getClass().getResourceAsStream("/rrd_graph_def_variables.xml");
        RrdGraphDefTemplate template = new RrdGraphDefTemplate(new InputSource(in));

        // Not all variables tested, more to be added
        template.setVariable("filename", "test.png");
        GregorianCalendar start = new GregorianCalendar(2020, 2, 25);
        GregorianCalendar end = new GregorianCalendar(2020, 2, 27);
        template.setVariable("start", start);
        template.setVariable("end", end);
        template.setVariable("width", 444);
        RrdGraphDef gdef = template.getRrdGraphDef();
        Assert.assertEquals(start.getTimeInMillis(), gdef.getStartTime() * 1000);
        Assert.assertEquals(end.getTimeInMillis(), gdef.getEndTime() * 1000);
        Assert.assertEquals("test.png", gdef.filename);
        Assert.assertEquals(444, gdef.width);
    }

    @Test
    public void testRrdDefString() throws IOException {
        RrdDef rrdDef = new RrdDef(testFolder.newFile().getAbsolutePath());
        rrdDef.setVersion(2);
        rrdDef.addDatasource("sun", GAUGE, 600, 0, Double.NaN);
        rrdDef.addDatasource("shade", GAUGE, 600, 0, Double.NaN);
        rrdDef.addArchive(AVERAGE, 0.5, 1, 600);
        rrdDef.addArchive(AVERAGE, 0.5, 6, 700);
        rrdDef.addArchive(AVERAGE, 0.5, 24, 775);
        rrdDef.addArchive(AVERAGE, 0.5, 288, 797);
        rrdDef.addArchive(MAX, 0.5, 1, 600);
        rrdDef.addArchive(MAX, 0.5, 6, 700);
        rrdDef.addArchive(MAX, 0.5, 24, 775);
        rrdDef.addArchive(MAX, 0.5, 288, 797);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        rrdDef.exportXmlTemplate(out);
        out.flush();
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        RrdDefTemplate template = new RrdDefTemplate(new InputSource(in));
        RrdDef rrdDef2 = template.getRrdDef();
        Assert.assertTrue("new RrdDef don't match", rrdDef2.equals(rrdDef));
    }

    @Test
    public void testRrdDefUri() throws IOException {
        RrdDef rrdDef = new RrdDef(testFolder.newFile().toURI());
        rrdDef.setVersion(2);
        rrdDef.addDatasource("sun", GAUGE, 600, 0, Double.NaN);
        rrdDef.addDatasource("shade", GAUGE, 600, 0, Double.NaN);
        rrdDef.addArchive(AVERAGE, 0.5, 1, 600);
        rrdDef.addArchive(AVERAGE, 0.5, 6, 700);
        rrdDef.addArchive(AVERAGE, 0.5, 24, 775);
        rrdDef.addArchive(AVERAGE, 0.5, 288, 797);
        rrdDef.addArchive(MAX, 0.5, 1, 600);
        rrdDef.addArchive(MAX, 0.5, 6, 700);
        rrdDef.addArchive(MAX, 0.5, 24, 775);
        rrdDef.addArchive(MAX, 0.5, 288, 797);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        rrdDef.exportXmlTemplate(out, false);
        out.flush();
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        RrdDefTemplate template = new RrdDefTemplate(new InputSource(in));
        RrdDef rrdDef2 = template.getRrdDef();
        Assert.assertTrue("new RrdDef don't match", rrdDef2.equals(rrdDef));
    }

    @Test
    public void testRrdDefFile() throws IOException {
        RrdDef rrdDef = new RrdDef(testFolder.newFile().toURI());
        rrdDef.setVersion(2);
        rrdDef.addDatasource("sun", GAUGE, 600, 0, Double.NaN);
        rrdDef.addDatasource("shade", GAUGE, 600, 0, Double.NaN);
        rrdDef.addArchive(AVERAGE, 0.5, 1, 600);
        rrdDef.addArchive(AVERAGE, 0.5, 6, 700);
        rrdDef.addArchive(AVERAGE, 0.5, 24, 775);
        rrdDef.addArchive(AVERAGE, 0.5, 288, 797);
        rrdDef.addArchive(MAX, 0.5, 1, 600);
        rrdDef.addArchive(MAX, 0.5, 6, 700);
        rrdDef.addArchive(MAX, 0.5, 24, 775);
        rrdDef.addArchive(MAX, 0.5, 288, 797);

        InputStream in = getClass().getResourceAsStream("/rrd_def.xml"); 
        RrdDefTemplate template = new RrdDefTemplate(new InputSource(in));
        RrdDef rrdDef2 = template.getRrdDef();
        Assert.assertTrue("new RrdDef don't match", rrdDef2.equals(rrdDef));
    }

}
