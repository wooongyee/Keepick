import { configureStore } from "@reduxjs/toolkit";
import authReducer from "@/features/auth/model/authSlice";
import { userReducer } from "@/entities/user";

export const makeStore = () => {
  return configureStore({
    reducer: {
      auth: authReducer,
      user: userReducer,
    },
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware({
        serializableCheck: {
          ignoredActions: ["persist/PERSIST"],
        },
      }),
  });
};

export type AppStore = ReturnType<typeof makeStore>;
export type RootState = ReturnType<AppStore["getState"]>;
export type AppDispatch = AppStore["dispatch"];
