package prototype.xd.scheduler.utilities.services;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class KeepAliveService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        BackgroundSetterService.ping(getApplicationContext(), false);
        return false;
    }
    
    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
