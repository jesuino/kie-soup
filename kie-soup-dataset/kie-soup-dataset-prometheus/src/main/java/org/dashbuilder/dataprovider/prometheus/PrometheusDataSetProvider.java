package org.dashbuilder.dataprovider.prometheus;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dashbuilder.DataSetCore;
import org.dashbuilder.dataprovider.DataSetProvider;
import org.dashbuilder.dataprovider.DataSetProviderType;
import org.dashbuilder.dataprovider.StaticDataSetProvider;
import org.dashbuilder.dataprovider.prometheus.client.PrometheusClient;
import org.dashbuilder.dataprovider.prometheus.client.QueryResponse;
import org.dashbuilder.dataprovider.prometheus.client.Result;
import org.dashbuilder.dataprovider.prometheus.client.ResultType;
import org.dashbuilder.dataprovider.prometheus.client.Status;
import org.dashbuilder.dataprovider.prometheus.client.Value;
import org.dashbuilder.dataset.ColumnType;
import org.dashbuilder.dataset.DataSet;
import org.dashbuilder.dataset.DataSetFactory;
import org.dashbuilder.dataset.DataSetLookup;
import org.dashbuilder.dataset.DataSetMetadata;
import org.dashbuilder.dataset.def.DataSetDef;
import org.dashbuilder.dataset.def.DataSetDefRegistry;
import org.dashbuilder.dataset.def.DataSetDefRegistryListener;
import org.dashbuilder.dataset.def.PrometheusDataSetDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrometheusDataSetProvider implements DataSetProvider, DataSetDefRegistryListener {

    public static final String VALUE_COLUMN = "VALUE";
    public static final String TIME_COLUMN = "TIME";

    protected StaticDataSetProvider staticDataSetProvider;
    protected Logger log = LoggerFactory.getLogger(PrometheusDataSetProvider.class);

    private static PrometheusDataSetProvider SINGLETON = null;

    public static PrometheusDataSetProvider get() {
        if (SINGLETON == null) {
            StaticDataSetProvider staticDataSetProvider = DataSetCore.get().getStaticDataSetProvider();
            DataSetDefRegistry dataSetDefRegistry = DataSetCore.get().getDataSetDefRegistry();
            SINGLETON = new PrometheusDataSetProvider(staticDataSetProvider);
            dataSetDefRegistry.addListener(SINGLETON);
        }
        return SINGLETON;
    }

    public PrometheusDataSetProvider() {}

    public PrometheusDataSetProvider(StaticDataSetProvider staticDataSetProvider) {
        this.staticDataSetProvider = staticDataSetProvider;
    }

    @SuppressWarnings("rawtypes")
    public DataSetProviderType getType() {
        return DataSetProviderType.PROMETHEUS;
    }

    public DataSetMetadata getDataSetMetadata(DataSetDef def) throws Exception {
        DataSet dataSet = lookupDataSet(def, null);
        if (dataSet == null) {
            return null;
        }
        return dataSet.getMetadata();
    }

    public DataSet lookupDataSet(DataSetDef def, DataSetLookup lookup) throws Exception {

        // TODO: Implement cache - if cache or test then return the dataset def from static 
        // - otherwise go to the server and get the dataset
        // Look first into the static data set provider since CSV data set are statically registered once loaded.
        String baseUrl = ((PrometheusDataSetDef) def).getServerUrl();
        String query = ((PrometheusDataSetDef) def).getQuery();
        QueryResponse response = new PrometheusClient(baseUrl).query(query);

        if (response.getStatus() == Status.ERROR) {
            throw new IllegalArgumentException("Error response received from Prometheus: " + response.getError());
        }

        DataSet dataSet = toDataSet(response);
        dataSet.setUUID(def.getUUID());
        dataSet.setDefinition(def);
        staticDataSetProvider.registerDataSet(dataSet);
        return staticDataSetProvider.lookupDataSet(def, lookup);
    }

    protected DataSet toDataSet(QueryResponse response) {
        DataSet dataSet = DataSetFactory.newEmptyDataSet();
        List<Result> results = response.getResults();

        Set<String> metricColumns = getMetricColumns(results);

        metricColumns.forEach(c -> dataSet.addColumn(c, ColumnType.LABEL));

        dataSet.addColumn(TIME_COLUMN, ColumnType.NUMBER);
        dataSet.addColumn(VALUE_COLUMN, response.getResultType() == ResultType.STRING
                ? ColumnType.TEXT
                : ColumnType.NUMBER);

        for (Result result : results) {
            for (Value value : result.getValues()) {
                Map<String, String> metric = result.getMetric();
                Object[] row = new Object[metric.size() + 2];
                int i = 0;
                for (String key : metricColumns) {
                    row[i++] = metric.get(key);
                }
                row[i++] = value.getTimestamp();
                row[i] = value.getValue();
                dataSet.addValuesAt(dataSet.getRowCount(), row);
            }
        }
        return dataSet;
    }

    private Set<String> getMetricColumns(List<Result> results) {
        return results.size() < 1 || results.get(0).getMetric() == null
                ? Collections.emptySet()
                : results.get(0).getMetric().keySet();
    }

    // Listen to changes on the data set definition registry
    @Override
    public void onDataSetDefStale(DataSetDef def) {
        staticDataSetProvider.removeDataSet(def.getUUID());
    }

    @Override
    public void onDataSetDefModified(DataSetDef olDef, DataSetDef newDef) {
        staticDataSetProvider.removeDataSet(olDef.getUUID());
    }

    @Override
    public void onDataSetDefRemoved(DataSetDef oldDef) {
        staticDataSetProvider.removeDataSet(oldDef.getUUID());
    }

    @Override
    public void onDataSetDefRegistered(DataSetDef newDef) {
        // empty
    }

    @Override
    public boolean isDataSetOutdated(DataSetDef def) {
        // perhaps check cache here
        return false;
    }
}
