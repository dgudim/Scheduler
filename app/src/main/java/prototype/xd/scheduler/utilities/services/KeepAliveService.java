package prototype.xd.scheduler.utilities.services;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class KeepAliveService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        BackgroundSetterService.keepAlive(getApplicationContext());
        System.out.println("---------------------------------------");
        System.out.println("---------------------------------------");
        System.out.println("---------------------------------------");
        System.out.println("---------------------------------------");
        System.out.println("---------------------------------------");
        System.out.println("---------------------------------------");
        System.out.println("---------------------------------------");
        System.out.println("---------------------------------------");
        return false;
    }
    
    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
