package prototype.xd.scheduler.adapters;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

public class BindingViewHolder<T extends ViewBinding> extends RecyclerView.ViewHolder {
    @NonNull
    protected final T binding;
    
    public BindingViewHolder(@NonNull T binding) {
        super(binding.getRoot());
        this.binding = binding;
    }
}
