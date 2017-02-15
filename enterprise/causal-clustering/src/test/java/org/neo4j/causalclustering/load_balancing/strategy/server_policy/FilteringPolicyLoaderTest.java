/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.causalclustering.load_balancing.strategy.server_policy;

import org.junit.Test;

import java.util.Map;

import org.neo4j.causalclustering.load_balancing.filters.Filter;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.logging.NullLogProvider;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.neo4j.causalclustering.core.CausalClusteringSettings.load_balancing_config;
import static org.neo4j.causalclustering.load_balancing.strategy.server_policy.FilterBuilder.filter;
import static org.neo4j.helpers.collection.MapUtil.stringMap;

public class FilteringPolicyLoaderTest
{
    @Test
    public void shouldLoadConfiguredPolicies() throws Exception
    {
        // given
        String strategyName = "server_policy";

        Object[][] input = {
                {
                        "asia_west",

                        "tags(asia_west) -> min(2);" +
                        "tags(asia) -> min(2);" +
                        "all()",

                        filter().tags( "asia_west" ).min( 2 ).newRule()
                                .tags( "asia" ).min( 2 ).newRule()
                                .all()
                                .build()
                },
                {
                        "asia_east",

                        "tags(asia_east) -> min(2);" +
                        "tags(asia) -> min(2);" +
                        "all()",

                        filter().tags( "asia_east" ).min( 2 ).newRule()
                                .tags( "asia" ).min( 2 ).newRule()
                                .all()
                                .build()
                },
        };

        Config config = Config.empty();

        for ( Object[] row : input )
        {
            String policyName = (String) row[0];
            String filterSpec = (String) row[1];
            config = config.augment( stringMap( configNameFor( strategyName, policyName ), filterSpec ) );
        }

        // when
        Policies policies = FilteringPolicyLoader.load( config, strategyName, NullLogProvider.getInstance() );

        // then
        for ( Object[] row : input )
        {
            String policyName = (String) row[0];
            Policy loadedPolicy = policies.selectFor( policyNameContext( policyName ) );
            @SuppressWarnings( "unchecked" )
            Policy expectedPolicy = new FilteringPolicy( (Filter<ServerInfo>) row[2] );
            assertEquals( expectedPolicy, loadedPolicy );
        }
    }

    private static Map<String,String> policyNameContext( String policyName )
    {
        return stringMap( Policies.POLICY_KEY, policyName );
    }

    private static String configNameFor( String strategyName, String policyName )
    {
        return format( "%s.%s.%s", load_balancing_config.name(), strategyName, policyName );
    }
}
