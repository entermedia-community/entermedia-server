package utils

import org.entermediadb.asset.util.MathUtils

public void runit()
{
	
	int mb = 1024*1024;
	
	Runtime runtime = Runtime.getRuntime();
	
	log.info("Before Used Memory: "
		+ MathUtils.divide(runtime.totalMemory() - runtime.freeMemory() , mb) + "mb"  );

	log.info("Before Free Memory: "
		+ MathUtils.divide(runtime.freeMemory() , mb) + "mb" );
	
	System.gc();
	System.runFinalization();

	log.info("After Used Memory: "
		+ MathUtils.divide(runtime.totalMemory() - runtime.freeMemory() , mb) + "mb"  );

	log.info("After Free Memory: "
		+ MathUtils.divide(runtime.freeMemory() , mb) + "mb" );

		
	
}

runit();

