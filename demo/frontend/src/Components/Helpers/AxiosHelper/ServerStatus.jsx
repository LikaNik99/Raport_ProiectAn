let serverDown = false;
const listeners = new Set();

export const setServerDown = () => {
    if (serverDown) return; // защита от спама
    serverDown = true;
    console.log("SERVER DOWN")
    listeners.forEach(fn => fn(true));
};

export const resetServerDown = () => {
    serverDown = false;
    listeners.forEach(fn => fn(false));
};

export const subscribeServerStatus = (fn) => {
    listeners.add(fn);
    return () => listeners.delete(fn);
};
