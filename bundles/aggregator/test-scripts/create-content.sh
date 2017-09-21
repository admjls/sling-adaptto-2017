# create content for manual testing of the aggregator
export BASE=http://localhost:8080
export U=admin:admin
curl -u $U -Fsling:resourceType=agg $BASE/apps/agg ; echo
curl -u $U -T agg.esp $BASE/apps/agg/agg.esp ; echo
curl -u $U -T SLING-CONTENT.esp $BASE/apps/agg/SLING-CONTENT.esp ; echo

echo Script output:
curl -u $U $BASE/apps/agg.html ; echo