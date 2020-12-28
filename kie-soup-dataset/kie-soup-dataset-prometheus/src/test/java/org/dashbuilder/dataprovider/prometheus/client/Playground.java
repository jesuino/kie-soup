package org.dashbuilder.dataprovider.prometheus.client;

import org.junit.Test;

public class Playground {
    
    @Test
    public void test() {
        PrometheusClient client = new PrometheusClient();
        QueryResponse query = client.query("kie_server_data_set_execution_time_seconds_count");
        System.out.println(query);
    }

}
