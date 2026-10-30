import { Router, type RequestHandler } from "express";

export interface ScreenShareControllerHandlers {
  startScreenShare: RequestHandler;
  stopScreenShare: RequestHandler;
  createConsumer: RequestHandler;
  getActiveScreenShares: RequestHandler;
  cleanupPeer: RequestHandler;
  cleanupRoom: RequestHandler;
  debugRoom: RequestHandler;
}

/**
 * Preserve the existing REST paths for screen-share recovery.
 * The original ScreenShareController is missing from the repository. Keep this
 * router unmounted until that controller is restored, then inject it here.
 */
export function screenShareRouter(controller: ScreenShareControllerHandlers): Router {
  const router = Router();

  router.post("/start", controller.startScreenShare.bind(controller));
  router.post("/stop", controller.stopScreenShare.bind(controller));
  router.post("/create-consumer", controller.createConsumer.bind(controller));
  router.get("/active/:roomId", controller.getActiveScreenShares.bind(controller));
  router.delete("/cleanup/:roomId/:peerId", controller.cleanupPeer.bind(controller));
  router.delete("/room/:roomId", controller.cleanupRoom.bind(controller));
  router.get("/debug/:roomId", controller.debugRoom.bind(controller));
  router.get("/health", (_req, res) => {
    res.status(200).json({
      success: true,
      message: "Screen share service is healthy",
      timestamp: new Date().toISOString(),
    });
  });

  return router;
}
